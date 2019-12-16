from jwt import encode, InvalidTokenError
from flask import Flask, request
from flask import jsonify
from flask_hal import document, HAL, link
from flask_hal.link import Collection
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
    "Jack", "85f293f02afec08cc90ec9b9501ff532c8c46c094850516700b5e8bd95bb570c")


@app.route("/publications", methods=["POST", "GET"])
def handle_pubs():
    if request.method == 'POST':
        if request.is_json:
            json = request.get_json()
            username = json['username']
            password = json['password']
            if username is None or password is None:
                return('<h1>FilesApi</h1>No login data', 400)
            title = json["title"]
            author = json["author"]
            publisher = json["publisher"]
            year = json['year']
            if title is None or author is None or publisher is None or year is None:
                return('<h1>FilesApi</h1>No needed data', 400)
            if redis_db.exists(username) and redis_db.get(username).decode() == password:
                id = redis_db.llen("publications:_+" + username)
                id = str(id)
                redis_db.rpush("publications:_+" + username,
                               title + ":_+" + id)
                redis_db.set("publications:_+" + username + ":_+" + id,
                             title + ":_+" + author + ":_+" + publisher + ":_+" + year)
                return ('OK', 200)
            else:
                return('<h1>Files API</h1>Invalid login data', 401)
        else:
            return('<h1>Files API</h1>Bad request', 400)
    else:
        username = request.args.get("username")
        password = request.args.get("password")
        if username == None or password == None:
            return('<h1>FilesApi</h1>No login data', 400)
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
                if len(data) > 4:
                    l = link.Link(data[0]+":_+"+id, data[4])
                    links.append(l)
                publications.append(publication)
            json = {
                "publications": publications
            }

            return document.Document(data=json, links=links)
        else:
            return('<h1>Files API</h1>Invalid login data', 401)


def prepare_publication(data):
    publication = data[0] + ":_+" + data[1] + ":_+" + data[2] + ":_+" + data[3]
    return publication


@app.route("/publications/<fid>", methods=["POST"])
def post_pub(fid):
    username = request.form.get("username")
    password = request.form.get("password")
    if username is None or password is None:
        return('<h1>FilesApi</h1>No login data', 400)
    if redis_db.exists(username) and redis_db.get(username).decode() == password:
        pub_title = fid
        file = request.files.get("file")
        filename = file.filename
        link = 'http://files:5000/download/' + filename
        id = "-1"
        for title in redis_db.lrange("publications:_+" + username, 0, redis_db.llen("publications:_+" + username)):
            title = title.decode()
            title = title.split(":_+")
            if title[0] == pub_title:
                id = title[1]
                break
        if id == '-1':
            return ('<h1>FilesApi</h1>Publication not found', 404)
        data = redis_db.get("publications:_+" + username + ":_+" + id).decode()
        data = data + ":_+" + link
        redis_db.set("publications:_+" + username + ":_+" + id,
                     data)
        form_url = "http://files:5000/upload"
        token = create_token()
        form_data = {
            "token": token,           
        }
        files = {
            "file": (filename, file)
        }
        answer = requests.post(form_url, data=form_data, files=files)
        file.close()
        if(answer.status_code == 401):
            return ('<h1>Files API</h1>Invalid upload token', 401)
        elif(answer.status_code == 400):
            return ('<h1>Files API</h1>No file given', 400)
        else:
            return('OK', 200)
    else:
        return('<h1>Files API</h1>Invalid login data', 401)


@app.route("/publications/<fid>/delete")
def deleteFile():
    # TODO usuwanie plików z bazy publikacji (api) i bazy plików (fileshare)
    return


def create_token():
    expiration = datetime.datetime.utcnow() + datetime.timedelta(seconds=JWT_TIME)
    return encode({"iss": "fileshare.company.com", "exp": expiration}, JWT_SECRET, "HS256")
