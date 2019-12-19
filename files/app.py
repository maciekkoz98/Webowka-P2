from jwt import decode, InvalidTokenError
from flask import Flask
from flask import request
from flask import make_response
from flask import render_template
from flask import jsonify
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
    return redirect(f"{callback}?fname={filename}") if callback \
        else ('<h1>Fileshare</h1> File added', 200)


@app.route('/download/<fid>')
def download(fid):
    token = request.args.get('token')
    if len(fid) == 0:
        return (f'<h1>Fileshare</h1> No file specified', 404)
    if token is None:
        return ('<h1>Fileshare</h1>No token provided', 401)
    file_id = check_token(token)
    if not file_id or file_id != fid:
        return('<h1>Fileshare</h1>Invalid token provided', 401)

    index = 0
    for name in redis_db.lrange("filenames", 0, redis_db.llen("filenames")):
        if name.decode() == fid:
            break
        index += 1

    response = make_response(redis_db.lindex("files", index), 200)
    response.headers['Content-type'] = "multipart/form-data"
    return response


@app.route('/delete/<fid>')
def delete(fid):
    token = request.args.get('token')
    if len(fid) == 0:
        return (f'<h1>Fileshare</h1> No file specified', 404)
    if token is None:
        return ('<h1>Fileshare</h1>No token provided', 401)
    if not check_token(token):
        return('<h1>Fileshare</h1>Invalid token provided', 401)

    index = 0
    guard = False
    for name in redis_db.lrange("filenames", 0, redis_db.llen("filenames")):
        if name.decode() == fid:
            redis_db.lrem(name="filenames", value=fid, count=0)
            guard = True
            break
        index += 1
    if not guard:
        return ('<h1>Fileshare</h1>File not found. Cannot be deleted.', 404)
    redis_db.lrem(name="files", value=redis_db.lindex("files", index), count=0)
    return ('<h1>Fileshare</h1>File deleted', 200)


@app.route('/filelist', methods=['POST'])
def getFileList():
    token_json = request.get_json()
    token = token_json['token']
    if check_token(token):
        filelist = redis_db.lrange("filenames", 0, redis_db.llen("filenames"))
        for i in range(0, len(filelist)):
            filelist[i] = filelist[i].decode()
        body = {
            "files": filelist
        }
        return jsonify(body)
    else:
        return (f'<h1>Fileshare</h1> Invalid token provided', 401)


def check_token(t):
    try:
        payload = decode(t, JWT_SECRET)
        try:
            if payload['action'] == "delete" or payload['action'] == "download":
                return payload['file_id']
            elif payload['action'] == "upload" or payload['action'] == "list":
                return True
            else:
                return False
        except KeyError:
            return False
    except InvalidTokenError:
        return False


def redirect(location):
    response = make_response('', 303)
    response.headers["Location"] = location
    return response
