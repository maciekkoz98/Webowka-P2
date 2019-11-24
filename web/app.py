from jwt import encode
from uuid import uuid4
from flask import Flask
from flask import request
from flask import make_response
from dotenv import load_dotenv
from os import getenv
import datetime
import redis
import hashlib

load_dotenv(verbose=True)


HTML = """<!doctype html>
<head>
    <title>Fileshare</title>
    <meta charset="utf-8"/>
</head>"""

app = Flask(__name__, static_url_path="/static")
FILE = getenv("FILESHARE")
WEB = getenv("WEB")
SESSION_TIME = int(getenv("SESSION_TIME"))
JWT_SESSION_TIME = int(getenv("JWT_SESSION_TIME"))
JWT_SECRET = getenv("JWT_SECRET")

redis_instance = redis.Redis(host="redis1", port=6379, db=0)


@app.route('/')
def index():
    redis_instance.set(
        "Jacek", "9f05d493a1dfa97972928cd617798090dc0e2465f044d01fc2d9dcbe749ea2a1")
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
    return f"""{HTML}
        <h1>Zaloguj się</h1>
        <h2>{error_message}</h2>
        <form action="/auth" method="POST">
            <input type="text" name="username" placeholder="Wprowadź login" />
            <input type="password" name="passwd" placeholder="Wprowadź hasło" />
            <input type="submit" />
        </form>"""


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
        upload_token = create_upl_token().decode('ascii')
        return f"""{HTML}
        <h1>Fileshare</h1>
        <h2>Lista załadowanych plików</h2>

        <h2>Prześlij plik</h2>
        <form action="{FILE}/upload" method="POST" enctype="multipart/form-data">
            <input type="file" name="file" />
            <input type="hidden" name="token" value="{upload_token}" />
            <input type="hidden" name="callback" value="{WEB}/callback" />
            <input type="submit" />
        </form> """
    else:
        return redirect("/login")


@app.route('/callback')
def callback():
    fname = request.args.get("fname")
    return (f"<h1>{fname}</h1>", 200)


def redirect(location):
    responce = make_response('', 303)
    responce.headers["Location"] = location
    return responce


def create_upl_token():
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=JWT_SESSION_TIME)
    return encode({"iss": "fileshare.company.com", "exp": expiration}, JWT_SECRET, "HS256")


def creat_downl_token():
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=JWT_SESSION_TIME)
    return encode({"iss": "web.company.com", "exp": expiration}, JWT_SECRET, "HS256")
