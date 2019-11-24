from jwt import decode, InvalidTokenError
from flask import Flask
from flask import request
from flask import make_response
from os import getenv
from dotenv import load_dotenv
import redis

load_dotenv(verbose=True)

app = Flask(__name__)
JWT_SECRET = getenv('JWT_SECRET')


HTML = """<!doctype html>
<head>
    <title>Fileshare</title>
    <meta charset="utf-8"/>
</head>"""

redis_instance = redis.Redis(host="redis2", port=6379, db=0)


@app.route('/upload', methods=['POST'])
def upload():
    file = request.files.get('file')
    token = request.form.get("token")
    callback = request.form.get("callback")

    if file is None:
        return redirect(f"{callback}?error=No+file") if callback \
            else ('<h1>Fileshare</h1> No file provided', 400)
    if token is None:
        return redirect(f"{callback}?error=No+token") if callback \
            else ('<h1>Fileshare</h1> No token provided', 401)
    if not check_token(token):
        return redirect(f"{callback}?error=Invalid+token") if callback \
            else ('<h1>Fileshare</h1> Invalid token provided', 401)

    # zapisz do redis
    redis_instance.lpush("files", file.read())
    return redirect(f"{callback}?fname={file.name}")


@app.route('/download/<fid>')
def download(fid):
    token = request.args.get('token')
    if len(fid) == 0:
        return

    return


def check_token(t):
    try:
        decode(t, JWT_SECRET)
    except InvalidTokenError:
        return False
    return True


def redirect(location):
    responce = make_response('', 303)
    responce.headers["Location"] = location
    return responce
