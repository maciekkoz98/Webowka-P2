from jwt import encode
from uuid import uuid4
from flask import Flask
from flask import request
from flask import make_response
from dotenv import load_dotenv
from os import getenv
import datetime
import redis

load_dotenv(verbose=True)


HTML = """<!doctype html>
<head><title>Fileshare</title><meta charset="utf-8"/></head>"""

app = Flask(__name__)
SESSION_TIME = int(getenv("SESSION_TIME"))


@app.route('/')
def index():
    session_id = request.cookies.get('session_id')
    response = redirect("/list" if session_id else "/login")
    return response


@app.route('/list')
def files():
    return


@app.route('/login')
def login():
    return f"""{HTML}
    <h1>Zaloguj się</h1>
    <form action="/auth" method="POST">
        <input type="text" name="username" placeholder="Wprowadź login" />
        <input type="password" name="passwd" placeholder="Wprowadź hasło" />
        <input type="submit" />
    </form>"""


@app.route('/auth', methods=['POST'])
def auth():
    redis_instance = redis.Redis(host="redis", port=6379, db=0)
    username = request.form.get('username')
    passwd = request.form.get('passwd')

    response = make_response('', 303)
    if redis_instance.exists(username):
        redis_pass = redis_instance.get(username)
        if passwd == redis_pass:
            session_id = str(uuid4())
            response.set_cookie("session_id", session_id, max_age=SESSION_TIME)
        else:
            response.set_cookie("session_id", "INVALIDATE", max_age=-1)
            response.headers["Location"] = "/login"
    else:
        response.set_cookie("session_id", "INVALIDATE", max_age=-1)
        response.headers["Location"] = "/login"

    return response


def redirect(location):
    responce = make_response('', 303)
    responce.headers["Location"] = location
    return responce
