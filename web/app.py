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
            redis_instance.set("session_id", username + " " + session_id)
            response.set_cookie("session_id", session_id, max_age=SESSION_TIME)
            response.headers["Location"] = "/list"
        else:
            return redirect(f"/login?error=Invalid+passwd")
    else:
        return redirect(f"/login?error=Invalid+login")

    return response


@app.route('/list')
def files():
    session_id = request.cookies.get('session_id')
    session_id_user_redis = redis_instance.get(
        "session_id").decode().split(" ")
    session_id_redis = session_id_user_redis[1]
    if session_id == session_id_redis:
        upload_token = create_token(JWT_SESSION_TIME).decode('ascii')
        download_token = create_token(JWT_SESSION_TIME).decode('ascii')
        list_token = create_token(10).decode('ascii')
        filelist_json = requests.post(
            "http://files:5000/filelist", json={"token": list_token}, verify=False).json()
        fileslist = filelist_json['files']
        api_url = get_api_url(session_id_user_redis)
        publications = requests.get(api_url).json()
        publications = publications["publications"]
        # TODO prezentacja publikacji

        return render_template("list.html", filelist=fileslist, publications=publications,
                               FILE=FILE, upload_token=upload_token,
                               download_token=download_token, WEB=WEB)
    else:
        return redirect("/login")


def get_api_url(session_id_user):
    url = "http://api:5000/publications"
    user = session_id_user[0]
    password = redis_instance.get(user).decode()
    return url + "?username=" + user + "&password=" + password


@app.route('/callback')
def callback():
    session_id = request.cookies.get("session_id")
    error = request.args.get("error")
    filename = request.args.get("fname")
    redis_session = redis_instance.get("session_id").decode().split(" ")
    redis_session = redis_session[1]
    if session_id != redis_session:
        return redirect("/login")

    if error:
        return f"<h1>APP</h1> Upload failed: {error}", 400

    return render_template("callback.html", filename=filename)


@app.route("/addPub", methods=["POST"])
def add_pub():
    session_id = request.cookies.get("session_id")
    redis_session = redis_instance.get("session_id").decode().split(" ")
    redis_session = redis_session[1]
    if session_id != redis_session:
        return redirect("/login")

    title = request.form.get("title")
    author = request.form.get("author")
    year = request.form.get("year")
    publisher = request.form.get("publisher")
    username = redis_instance.get("session_id").decode().split(" ")
    username = username[0]
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
    redis_instance.delete("session_id")
    response.set_cookie("session_id", "INVALIDATE", max_age=-1)
    return response


def redirect(location):
    responce = make_response('', 303)
    responce.headers["Location"] = location
    return responce


def create_token(time):
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=time)
    return encode({"iss": "fileshare.company.com", "exp": expiration}, JWT_SECRET, "HS256")
