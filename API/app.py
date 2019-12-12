from jwt import decode, InvalidTokenError
from flask import Flask, request
from flask import jsonify
import redis

app = Flask(__name__)

redis_db = redis.Redis(host="redis3", port=6379, db=0)
redis_db.set(
    "Jack", "85f293f02afec08cc90ec9b9501ff532c8c46c094850516700b5e8bd95bb570c")


@app.route("/publications", methods=["POST", "GET"])
def handle_pubs():
    if request.method == 'POST':
        if request.is_json:
            #TODO validation
            json = request.get_json()
            username = json['username']
            password = json['password']
            title = json["title"]
            author = json["author"]
            publisher = json["publisher"]
            year = json['year']

            if redis_db.exists(username) and redis_db.get(username).decode() == password:
                id = redis_db.llen("publications:" + username)
                id = str(id)
                redis_db.rpush("publications:" + username,
                               title + ":" + id)
                redis_db.set("publications:" + username + ":" +
                             id, title + ":" + author + ":" + publisher + ":" + year)
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
            for title in redis_db.lrange("publications:" + username, 0, redis_db.llen("publications:" + username)):
                title = title.decode().split(":")
                id = title[1]
                title = title[0]
                data = redis_db.get("publications:" + username + ":" + id)
                publication = data.decode()
                publications.append(publication)
            json = {
                "publications": publications
            }
            return jsonify(json)
        else:
            return('<h1>Files API</h1>Invalid login data', 401)


@app.route("/publications/<fid>")
def get_info(fid):
    # fid to numer id <-z bazy po nazwie pliku!
    return
