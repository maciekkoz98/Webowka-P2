from jwt import encode, InvalidTokenError
from flask import Flask, request
from flask import jsonify
from flask_hal import document, HAL, link
from flask_hal.link import Collection
from flask import make_response
from os import getenv
from dotenv import load_dotenv
import redis
import datetime
import requests

load_dotenv(verbose=True)
JWT_SECRET = getenv("JWT_SECRET")
JWT_TIME = int(getenv("JWT_TIME"))

app = Flask(__name__)
HAL(app)

redis_db = redis.Redis(host="redis3", port=6379, db=0)
redis_db.set(
    "jack@pw.edu.pl", "85f293f02afec08cc90ec9b9501ff532c8c46c094850516700b5e8bd95bb570c")


@app.route("/publications", methods=["POST", "GET"])
def handle_pubs():
    if request.method == 'POST':
        if request.is_json:
            json = request.get_json()
            username = json['username']
            password = json['password']
            if username is None or password is None:
                return('<h1>FilesApi</h1>No login data', 401)
            title = json["title"]
            author = json["author"]
            publisher = json["publisher"]
            year = json['year']
            if title is None or author is None or publisher is None or year is None:
                return('<h1>FilesApi</h1>No needed data', 400)
            if redis_db.exists(username) and redis_db.get(username).decode() == password:
                id = prepare_id(username)
                redis_db.rpush("publications:_+" + username,
                               title + ":_+" + id)
                redis_db.set("publications:_+" + username + ":_+" + id,
                             id + ":_+" + title + ":_+" + author + ":_+" + publisher + ":_+" + year)
                return ('OK', 200)
            else:
                return('<h1>Files API</h1>Invalid login data', 401)
        else:
            return('<h1>Files API</h1>Bad request', 400)
    else:
        username = request.args.get("username")
        password = request.args.get("password")
        if username == None or password == None:
            return('<h1>FilesApi</h1>No login data', 401)
        if redis_db.exists(username) and redis_db.get(username).decode() == password:
            publications = []
            links = Collection()
            for title in redis_db.lrange("publications:_+" + username, 0, redis_db.llen("publications:_+" + username)):
                title = title.decode().split(":_+")
                id = title[1]
                title = title[0]
                data = redis_db.get("publications:_+" + username + ":_+" + id)
                data = data.decode().split(":_+")
                publication = prepare_publication(data)
                if len(data) > 5:
                    l = link.Link(data[0] + ":_+download", data[5])
                    links.append(l)
                    d = link.Link(data[0] + ":_+delete", data[6])
                    links.append(d)
                publications.append(publication)
            json = {
                "publications": publications
            }

            return document.Document(data=json, links=links)
        else:
            return('<h1>Files API</h1>Invalid login data', 401)


def prepare_id(username):
    size = redis_db.llen("publications:_+" + username)
    last_pub = redis_db.lrange("publications:_+" + username, size-1, size)
    if len(last_pub) == 0:
        return '0'
    else:
        record = last_pub[0].decode().split(":_+")
        id = int(record[1])
        id = id + 1
        return str(id)


def prepare_publication(data):
    publication = data[0] + ":_+" + data[1] + ":_+" + \
        data[2] + ":_+" + data[3] + ":_+" + data[4]
    return publication


@app.route("/publications/<pub_id>", methods=["POST"])
def post_pub(pub_id):
    username = request.form.get("username")
    password = request.form.get("password")
    if username is None or password is None:
        return('<h1>FilesApi</h1>No login data', 401)
    if redis_db.exists(username) and redis_db.get(username).decode() == password:
        pub_id = str(pub_id)
        file = request.files.get("file")
        filename = file.filename
        download_link = 'https://fileshare.company.com/download/' + filename
        delete_link = 'https://filesapi.company.com/publications/' + \
            filename + '/delete?pid=' + pub_id
        data = redis_db.get("publications:_+" + username +
                            ":_+" + pub_id)
        if data is None:
            return ('<h1>FilesApi</h1>Publication not found', 404)
        data = data.decode()
        data = data + ":_+" + download_link + ":_+" + delete_link
        redis_db.set("publications:_+" + username + ":_+" + pub_id,
                     data)

        answer = upload_file(file, filename)
        if(answer.status_code == 401):
            return ('<h1>Files API</h1>Invalid upload token', 401)
        elif(answer.status_code == 400):
            return ('<h1>Files API</h1>No file given', 400)
        else:
            return('OK', 200)
    else:
        return('<h1>Files API</h1>Invalid login data', 401)


def upload_file(file, filename):
    form_url = "http://files:5000/upload"
    token = create_upload_token().decode('ascii')
    form_data = {
        "token": token,
    }
    files = {
        "file": (filename, file)
    }
    answer = requests.post(form_url, data=form_data, files=files)
    file.close()
    return answer


@app.route("/publications/<file_id>/delete")
def delete_file(file_id):
    username = request.args.get("username")
    password = request.args.get("password")
    if username is None or password is None:
        return('<h1>FilesApi</h1>No login data', 401)
    if redis_db.exists(username) and redis_db.get(username).decode() == password:
        pub_id = request.args.get("pid")
        if not check_fid_pub(username, pub_id, file_id):
            return('<h1>API</h1>Cannot delete file. Bad request', 400)
        token = create_delete_token(file_id).decode('ascii')
        ans = requests.get("http://files:5000/delete/" +
                           file_id + "?token=" + token)
        if ans.status_code == 404:
            return ('<h1>API</h1>Cannot delete file that not exists', 404)
        data = redis_db.get("publications:_+" + username +
                            ":_+" + pub_id)
        data = data.decode()
        data = data.split(":_+")
        data = prepare_publication(data)
        redis_db.set("publications:_+" + username + ":_+" + pub_id,
                     data)
        return ('<h1>Files API</h1>Succesfully deleted file: ' + file_id, 200)
    else:
        return('<h1>Files API</h1>Invalid login data', 401)


def check_fid_pub(username, pub_id, file_id):
    data = redis_db.get("publications:_+" + username +
                        ":_+" + pub_id)
    if data is None:
        return False
    data = data.decode()
    data = data.split(":_+")
    if len(data) < 7:
        return False
    delete_link = data[6]
    delete_link = delete_link[42:]
    if delete_link.find(file_id) == 0:
        cut = len(file_id) + 12
        pub_tocheck = delete_link[cut:]
        if pub_id == pub_tocheck:
            return True
        else:
            return False
    else:
        return False


@app.route("/publications/delete/<pub_id>")
def delete_publication(pub_id):
    username = request.args.get("username")
    password = request.args.get("password")
    if username is None or password is None:
        return('<h1>FilesApi</h1>No login data', 401)
    if redis_db.exists(username) and redis_db.get(username).decode() == password:
        data = redis_db.get("publications:_+" + username + ":_+" + pub_id)
        if data is None:
            return ('<h1>FilesApi</h1>Publication not found', 404)
        data = data.decode().split(":_+")
        if len(data) > 5:
            url = "http://api:5000" + data[6][28:]
            url = url + "&username=" + username + "&password=" + password
            ans = requests.get(url)
            if ans.status_code == 400:
                return("Error", 400)
            elif ans.status_code == 404:
                return("File not exists", 404)
            elif ans.status_code == 401:
                return("Logs", 401)
        redis_db.delete("publications:_+" + username + ":_+" + pub_id)
        title_id = data[1] + ":_+" + data[0]
        for name in redis_db.lrange("publications:_+" + username, 0, redis_db.llen("publications:_+" + username)):
            if name.decode() == title_id:
                redis_db.lrem(name="publications:_+" +
                              username, value=title_id, count=0)
                break
        return ('<h1>Files API</h1>Succesfully deleted publication: ' + pub_id, 200)
    else:
        return('<h1>Files API</h1>Invalid login data', 401)


def create_upload_token():
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=JWT_TIME)
    return encode({"iss": "fileshare.company.com", "exp": expiration, "action": "upload"}, JWT_SECRET, "HS256")


def create_delete_token(file_id):
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=JWT_TIME)
    return encode({"iss": "fileshare.company.com", "exp": expiration, "action": "delete", "file_id": file_id}, JWT_SECRET, "HS256")
