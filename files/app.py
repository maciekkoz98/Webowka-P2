from jwt import decode, InvalidTokenError
from flask import Flask
from flask import request
from flask import make_response
from flask import render_template
from os import getenv
from dotenv import load_dotenv
import redis

load_dotenv(verbose=True)

app = Flask(__name__)
JWT_SECRET = getenv('JWT_SECRET')

redis_db = redis.Redis(host="redis2", port=6379, db=0)


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

    redis_db.rpush("files", file.read())
    filename = file.filename
    redis_db.rpush("filenames", filename)
    file.close()
    return redirect(f"{callback}?fname={filename}")


@app.route('/download/<fid>')
def download(fid):
    token = request.args.get('token')
    if len(fid) == 0:
        return (f'<h1>Fileshare</h1> No file specified', 404)
    if token is None:
        return ('<h1>Fileshare</h1>No token provided', 401)
    if not check_token(token):
        return('<h1>Fileshare</h1>Invalid token provided', 401)

    index = 0
    for name in redis_db.lrange("filenames", 0, redis_db.llen("filenames")):
        if name.decode() == fid:
            break
        index += 1

    response = make_response(redis_db.lindex("files", index), 200)
    response.headers['Content-type'] = "multipart/form-data"
    return response
    # return render_template("try.html", filelist=redis_db.lrange("filenames", 0, redis_db.llen("filenames")))


@app.route('/files')
def getFileList():
    token = request.args.get('token')
    if check_token(token):
        response = make_response('', 303)
        files = redis_db.lrange("filenames", 0, redis_db.llen("filenames"))
        response.set_cookie("filelist", files, max_age=10)
        response.headers["Location"] = "https://web.company.com/list"
        return response
    else:
        return (f'<h1>Fileshare</h1> Invalid token provided', 401)


def check_token(t):
    try:
        decode(t, JWT_SECRET)
    except InvalidTokenError:
        return False
    return True


def redirect(location):
    response = make_response('', 303)
    response.headers["Location"] = location
    return response
