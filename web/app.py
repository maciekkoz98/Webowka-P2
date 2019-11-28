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

load_dotenv(verbose=True)

app = Flask(__name__, static_url_path="/static")
FILE = getenv("FILESHARE")
WEB = getenv("WEB")
SESSION_TIME = int(getenv("SESSION_TIME"))
JWT_SESSION_TIME = int(getenv("JWT_SESSION_TIME"))
JWT_SECRET = getenv("JWT_SECRET")

redis_instance = redis.Redis(host="redis1", port=6379, db=0)
fileslist = []


@app.route('/')
def index():
    redis_instance.set(
        "Jack", "85f293f02afec08cc90ec9b9501ff532c8c46c094850516700b5e8bd95bb570c")
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
            redis_instance.set("session_id", session_id)
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
    session_id_redis = redis_instance.get("session_id")
    if session_id == session_id_redis.decode():
        upload_token = create_token().decode('ascii')
        download_token = create_token().decode('ascii')
        filelist = request.cookies.get('filelist')
        if filelist is None:
            filelist = []
        return render_template("list.html", filelist=fileslist,
                               FILE=FILE, upload_token=upload_token,
                               download_token=download_token, WEB=WEB)
    else:
        return redirect("/login")


@app.route('/callback')
def callback():
    session_id = request.cookies.get("session_id")
    error = request.args.get("error")
    filename = request.args.get("fname")
    if session_id != redis_instance.get("session_id").decode():
        return redirect("/login")

    if error:
        return f"<h1>APP</h1> Upload failed: {error}", 400

    fileslist.append(filename)
    return render_template("callback.html", filename=filename)


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


def create_token():
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=JWT_SESSION_TIME)
    return encode({"iss": "fileshare.company.com", "exp": expiration}, JWT_SECRET, "HS256")
