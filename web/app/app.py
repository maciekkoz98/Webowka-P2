from jwt import encode
from uuid import uuid4
from flask import (
    Flask,
    request,
    make_response,
    render_template,
    Response,
    jsonify,
    session,
    redirect,
    url_for)
from functools import wraps
from dotenv import load_dotenv
from os import getenv
import datetime
import redis
import hashlib
import requests
import json
from authlib.integrations.flask_client import OAuth
from six.moves.urllib.parse import urlencode

load_dotenv(verbose=True)

redis_instance = redis.Redis(host="redis1", port=6379, db=0)

app = Flask(__name__, static_url_path="/static")
app.secret_key = 'twojastara'
oauth = OAuth(app)
WEB = getenv("WEB")
SESSION_TIME = int(getenv("SESSION_TIME"))
JWT_SESSION_TIME = int(getenv("JWT_SESSION_TIME"))
JWT_SECRET = getenv("JWT_SECRET")

auth0 = oauth.register(
    'auth0',
    client_id='IkBNSWsj6uC4c3WKMKQ6R4ngPH7PnW39',
    client_secret='2vweJGJo1Gwf9mqMF8ETvQvnW2Ff4r9e7Y7J1TjiLk0TJEXHfdbLKEBVYFVks8Xn',
    api_base_url='https://webowkap4.eu.auth0.com',
    access_token_url='https://webowkap4.eu.auth0.com/oauth/token',
    authorize_url='https://webowkap4.eu.auth0.com/authorize',
    client_kwargs={
        'scope': 'openid profile email',
    }
)


def event_stream():
    pubsub = redis_instance.pubsub(ignore_subscribe_messages=True)
    pubsub.subscribe('publications:'+session['profile']['name'])
    for message in pubsub.listen():
        return 'data: %s\n\n' % message['data'].decode()


def requires_auth(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        if 'profile' not in session:
            return redirect('/')
        return f(*args, **kwargs)
    return decorated


@app.route('/')
def index():
    return render_template("login_oauth.html")


@app.route('/login')
def login():
    return auth0.authorize_redirect(redirect_uri='https://web.company.com/loginCallback')


@app.route('/loginCallback')
def callback():
    auth0.authorize_access_token()
    resp = auth0.get('userinfo')
    userinfo = resp.json()
    session['jwt_payload'] = userinfo
    session['profile'] = {
        'user_id': userinfo['sub'],
        'name': userinfo['name'],
        'picture': userinfo['picture']
    }
    return redirect('/list')


@app.route('/list')
@requires_auth
def files():
    username = session['profile']['name']
    api_url = get_api_url(username)
    pubs_token = create_list_pubs_token().decode('ascii')
    pub_json = requests.get(
        api_url, headers={'Authorization': 'Bearer ' + pubs_token})
    pub_json = pub_json.json()

    publications = prepare_publications(pub_json)
    return render_template("list.html", publications=publications)


def get_api_url(user):
    url = "http://api:5000/publications"
    return url + "?username=" + user


def prepare_publications(pub_json):
    publications = pub_json["publications"]
    links = pub_json["_links"]
    for i in range(0, len(publications)):
        publications[i] = publications[i].split(":_+")
        try:
            dwn_link = links[publications[i][0] + ":_+download"]['href']
            del_link = links[publications[i][0] + ":_+delete"]['href']
            publications[i].append(dwn_link)
            file_name = dwn_link[39:]
            publications[i].append(file_name)
            publications[i].append(del_link)
        except KeyError:
            continue
    return publications


@app.route('/callback')
@requires_auth
def callback_files():
    error = request.args.get("error")
    filename = request.args.get("fname")
    if error:
        return f"<h1>APP</h1> Upload failed: {error}", 400

    return render_template("callback.html", filename=filename)


@app.route("/addPub", methods=["POST"])
@requires_auth
def add_pub():
    username = session['profile']['name']
    title = request.form.get("title")
    author = request.form.get("author")
    year = request.form.get("year")
    publisher = request.form.get("publisher")
    body = {
        "title": title,
        "author": author,
        "year": year,
        "publisher": publisher,
        "username": username
    }
    token = create_add_pub_token(
        title+author+year+publisher).decode('ascii')
    answer = requests.post("http://api:5000/publications",
                           json=body, headers={'Authorization': 'Bearer ' + token})
    if (answer.status_code == 200):
        redis_instance.publish('publications:'+username, title)
        return redirectTo("/list")
    else:
        return ('nie ok', 400)


@app.route('/stream')
def stream():
    return Response(event_stream(), mimetype="text/event-stream")


@app.route('/logout')
def logout():
    session.clear()
    params = {'returnTo': 'https://web.company.com/',
              'client_id': 'IkBNSWsj6uC4c3WKMKQ6R4ngPH7PnW39'}
    return redirect(auth0.api_base_url + '/v2/logout?' + urlencode(params))


@app.route("/sendToAPI", methods=["POST"])
@requires_auth
def attach_file():
    username = session['profile']['name']
    pub_id = request.form.get("pid")
    link = "http://api:5000/publications/" + pub_id
    file = request.files.get("file")
    form_data = {
        "username": username,
    }
    files = {
        "file": (file.filename, file)
    }
    token = create_add_file_token(file.filename).decode('ascii')
    ans = requests.post(link, files=files, data=form_data, headers={
                        'Authorization': 'Bearer ' + token})
    if ans.status_code == 200:
        return redirectTo('/list')
    else:
        return ("<h1>Web</h1>Bad request", 400)


@app.route("/deletePub")
@requires_auth
def deletePub():
    pub_id = request.args.get("pid")
    if pub_id is None:
        redirectTo("/list")

    username = session['profile']['name']
    token = create_delete_pub_token(pub_id).decode('ascii')
    link = "http://api:5000/publications/delete/" + pub_id + "?username=" + username
    ans = requests.get(link, headers={'Authorization': 'Bearer ' + token})
    if ans.status_code == 200:
        return redirectTo('/list')
    else:
        return ("<h1>Web</h1>Bad request", 400)


@app.route("/delFile", methods=["POST"])
@requires_auth
def delFile():
    link = request.form.get("link")
    link = "http://api:5000" + link[28:]
    username = session['profile']['name']
    token = create_delete_file_token().decode('ascii')
    requests.get(link+"&username="+username,
                 headers={'Authorization': 'Bearer ' + token})
    return redirectTo('/list')


@app.route("/fileDownload", methods=["POST"])
@requires_auth
def downloadFile():
    link = request.form.get("link")
    file_id = link[39:]
    token = create_download_token(file_id, 10).decode('ascii')
    return redirectTo(link + '?token=' + token)


def redirectTo(location):
    response = make_response('', 303)
    response.headers["Location"] = location
    return response


def create_download_token(file_id, time):
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=time)
    return encode({"iss": "fileshare.company.com", "exp": expiration, "action": "download", "file_id": file_id}, JWT_SECRET, "HS256")


def create_add_pub_token(publication):
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=30)
    return encode({"iss": 'filesapi.company.com', "exp": expiration, "action": "addPub", "publication": publication}, JWT_SECRET, "HS256")


def create_list_pubs_token():
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=30)
    return encode({"iss": 'filesapi.company.com', "exp": expiration, "action": "listPubs"}, JWT_SECRET, "HS256")


def create_add_file_token(filename):
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=30)
    return encode({"iss": 'filesapi.company.com', "exp": expiration, "action": "addFile", "filename": filename}, JWT_SECRET, "HS256")


def create_delete_file_token():
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=30)
    return encode({"iss": 'filesapi.company.com', "exp": expiration, "action": "deleteFile"}, JWT_SECRET, "HS256")


def create_delete_pub_token(pid):
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=30)
    return encode({"iss": 'filesapi.company.com', "exp": expiration, "action": "deletePub", "pubID": pid}, JWT_SECRET, "HS256")
