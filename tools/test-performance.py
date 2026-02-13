#!/usr/bin/env python3

import json
import random
import time
import urllib.parse
import urllib.request
import http.client
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed


BASE_URL = "http://localhost:8082"
SERVER_HOST = "localhost:5055"
USER = {"email": "admin", "password": "admin"}
DEVICES = 200
CREATE_WORKERS = 32
SEND_INTERVAL = 1.0


def login():
    req  = urllib.request.Request(f"{BASE_URL}/api/session")
    data = urllib.parse.urlencode(USER).encode()
    with urllib.request.urlopen(req, data) as resp:
        return resp.headers["Set-Cookie"]


def add_device(cookie: str, uid: str):
    req = urllib.request.Request(
        f"{BASE_URL}/api/devices",
        headers={"Cookie": cookie, "Content-Type": "application/json"},
        data=json.dumps({"name": uid, "uniqueId": uid}).encode()
    )
    try:
        urllib.request.urlopen(req)
    except Exception:
        pass


def send_message(conn: http.client.HTTPConnection, uid: str):
    params = urllib.parse.urlencode({
        "id":  uid,
        "lat": random.uniform(59.0, 61.0),
        "lon": random.uniform(29.0, 31.0),
    })
    try:
        conn.request("GET", f"?{params}")
        conn.getresponse().read()
    except Exception:
        conn.close()
        conn.connect()


def create_devices(cookie: str, ids):
    with ThreadPoolExecutor(max_workers=CREATE_WORKERS) as pool:
        futures = [pool.submit(add_device, cookie, uid) for uid in ids]
        for f in as_completed(futures):
            f.result()


def position_worker(uid: str):
    conn = http.client.HTTPConnection(SERVER_HOST, timeout=5)
    while True:
        send_message(conn, uid)
        time.sleep(SEND_INTERVAL)


def main():
    cookie = login()
    ids = [f"{i:06d}" for i in range(DEVICES)]

    create_devices(cookie, ids)

    for uid in ids:
        t = threading.Thread(target=position_worker, args=(uid,), daemon=True)
        t.start()

    try:
        while True:
            time.sleep(1000)
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
