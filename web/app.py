from jwt import encode
from uuid import uuid4
from flask import Flask
from flask import request
from flask import make_response
from flask import render_template
from dotenv import load_dotenv
from os import getenv
import datetime
import redis
import hashlib
import requests

load_dotenv(verbose=True)

app = Flask(__name__, static_url_path="/static")
FILE = getenv("FILESHARE")
WEB = getenv("WEB")
SESSION_TIME = int(getenv("SESSION_TIME"))
JWT_SESSION_TIME = int(getenv("JWT_SESSION_TIME"))
JWT_SECRET = getenv("JWT_SECRET")

redis_instance = redis.Redis(host="redis1", port=6379, db=0)
redis_instance.set(
    "Jack", "85f293f02afec08cc90ec9b9501ff532c8c46c094850516700b5e8bd95bb570c")


@app.route('/')
def index():
    session_id = request.cookies.get('session_id')
    response = redirect("/list" if session_id else "/login")
    return response


@app.route('/login')
def login():
    error = request.args.get("error")
    error_message = ""
    if error == "Invalid login":
        error_message = "Podany login jest nieprawidłowy"
    elif error == "Invalid passwd":
        error_message = "Podane hasło jest nieprawidłowe"
    return render_template("login.html", error_message=error_message)


@app.route('/auth', methods=['POST'])
def auth():
    username = request.form.get('username')
    passwd = request.form.get('passwd').encode()
    passwd = hashlib.sha256(passwd).hexdigest()

    response = make_response('', 303)
    if redis_instance.exists(username):
        redis_pass = redis_instance.get(username).decode()
        if passwd == redis_pass:
            session_id = str(uuid4())
            redis_instance.set(username + "_session_id", session_id)
            response.set_cookie("session_id", session_id,
                                max_age=SESSION_TIME, httponly=True, secure=True, samesite='Strict')
            response.set_cookie(
                "username", username, max_age=SESSION_TIME, httponly=True, secure=True, samesite='Strict')
            response.headers["Location"] = "/list"
        else:
            return redirect(f"/login?error=Invalid+passwd")
    else:
        return redirect(f"/login?error=Invalid+login")

    return response


@app.route('/list')
def files():
    if not check_user():
        return redirect("/login")

    username = request.cookies.get("username")
    upload_token = create_upload_token(JWT_SESSION_TIME).decode('ascii')
    list_token = create_list_token(10).decode('ascii')
    filelist_json = requests.post(
        "http://files:5000/filelist", json={"token": list_token}, verify=False).json()
    fileslist = filelist_json['files']
    api_url = get_api_url(username)
    pub_json = requests.get(api_url).json()

    publications = prepare_publications(pub_json)
    API = "https://filesapi.company.com/publications/"
    password = redis_instance.get(username).decode()
    return render_template("list.html", filelist=fileslist, publications=publications,
                           FILE=FILE, upload_token=upload_token, WEB=WEB, API=API, username=username, password=password)


def get_api_url(user):
    url = "http://api:5000/publications"
    password = redis_instance.get(user).decode()
    return url + "?username=" + user + "&password=" + password


def prepare_publications(pub_json):
    publications = pub_json["publications"]
    links = pub_json["_links"]
    for i in range(0, len(publications)):
        publications[i] = publications[i].split(":_+")
        try:
            dwn_link = links[str(i) + ":_+download"]['href']
            del_link = links[str(i) + ":_+delete"]['href']
            publications[i].append(dwn_link)
            file_name = dwn_link[39:]
            publications[i].append(file_name)
            publications[i].append(del_link)
        except KeyError:
            continue
    return publications


@app.route('/callback')
def callback():
    if not check_user():
        return redirect("/login")

    error = request.args.get("error")
    filename = request.args.get("fname")
    if error:
        return f"<h1>APP</h1> Upload failed: {error}", 400

    return render_template("callback.html", filename=filename)


@app.route("/addPub", methods=["POST"])
def add_pub():
    if not check_user():
        return redirect("/login")

    username = request.cookies.get("username")
    title = request.form.get("title")
    author = request.form.get("author")
    year = request.form.get("year")
    publisher = request.form.get("publisher")
    password = redis_instance.get(username).decode()
    body = {
        "title": title,
        "author": author,
        "year": year,
        "publisher": publisher,
        "username": username,
        "password": password
    }
    answer = requests.post("http://api:5000/publications", json=body)
    if (answer.status_code == 200):
        return redirect("/list")
    else:
        return ('nie ok', 400)


@app.route('/logout')
def logout():
    response = redirect('/login')
    username = request.cookies.get("username")
    redis_instance.delete(username + "_session_id")
    response.set_cookie("session_id", "INVALIDATE", max_age=-1)
    response.set_cookie("username", "INVALIDATE", max_age=-1)
    return response


@app.route('/addFileToPub')
def show_form():
    if not check_user():
        return redirect("/login")

    username = request.cookies.get("username")
    pub_id = request.args.get("pid")
    password = redis_instance.get(username).decode()
    address = "http://api:5000/publications/" + pub_id
    return render_template("fileForm.html", API=address, username=username, password=password)


@app.route("/sendToAPI", methods=["POST"])
def attach_file():
    if not check_user():
        return redirect("/login")

    username = request.form.get("username")
    password = request.form.get("password")
    pub_id = request.form.get("pid")
    link = "http://api:5000/publications/" + pub_id
    file = request.files.get("file")
    form_data = {
        "username": username,
        "password": password,
    }
    files = {
        "file": (file.filename, file)
    }

    ans = requests.post(link, data=form_data, files=files)
    if ans.status_code == 200:
        return redirect('/list')
    else:
        return ("<h1>Web</h1>Bad request", 400)


@app.route("/deletePub")
def deletePub():
    if not check_user():
        redirect('/login')
    pub_id = request.args.get("pid")
    if pub_id is None:
        redirect("/list")

    username = request.cookies.get("username")
    password = redis_instance.get(username).decode()
    link = "http://api:5000/publications/delete/" + pub_id + \
        "?username=" + username + "&password=" + password
    ans = requests.get(link)
    if ans.status_code == 200:
        return redirect('/list')
    else:
        return ("<h1>Web</h1>Bad request", 400)


@app.route("/delFile", methods=["POST"])
def delFile():
    if not check_user():
        return redirect('/login')
    link = request.form.get("link")
    username = request.cookies.get("username")
    password = redis_instance.get(username).decode()
    requests.get(link + "&username=" + username + "&password=" + password)
    return redirect('/list')


@app.route("/downloadFile", methods=["POST"])
def downloadFile():
    if not check_user():
        return redirect('/login')
    link = request.form.get("link")
    file_id = link[39:]
    token = create_download_token(file_id, 10).decode('ascii')
    return redirect(link + '?token=' + token)


def redirect(location):
    response = make_response('', 303)
    response.headers["Location"] = location
    return response


def create_list_token(time):
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=time)
    return encode({"iss": "fileshare.company.com", "exp": expiration, "action": "list"}, JWT_SECRET, "HS256")


def create_upload_token(time):
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=time)
    return encode({"iss": "fileshare.company.com", "exp": expiration, "action": "upload"}, JWT_SECRET, "HS256")


def create_download_token(file_id, time):
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=time)
    return encode({"iss": "fileshare.company.com", "exp": expiration, "action": "download", "file_id": file_id}, JWT_SECRET, "HS256")


def check_user():
    session_id = request.cookies.get("session_id")
    username = request.cookies.get("username")
    if username is None:
        return False
    redis_session = redis_instance.get(username + "_session_id")
    if redis_session is None:
        return False
    redis_session = redis_session.decode()
    if session_id != redis_session:
        return False
    return True
