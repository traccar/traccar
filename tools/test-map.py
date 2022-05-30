#!/usr/bin/python

import urllib
import urllib2
import httplib
import time
import random
import json

server = 'localhost:5055'
baseUrl = 'http://localhost:8082'
user = { 'email' : 'admin', 'password' : 'admin' }
devices = 500

def login():
    request = urllib2.Request(baseUrl + '/api/session')
    response = urllib2.urlopen(request, urllib.urlencode(user))
    return response.headers.get('Set-Cookie')

def add_device(cookie, unique_id):
    request = urllib2.Request(baseUrl + '/api/devices')
    request.add_header('Cookie', cookie)
    request.add_header('Content-Type', 'application/json')
    device = { 'name' : unique_id, 'uniqueId' : unique_id }
    try:
        response = urllib2.urlopen(request, json.dumps(device))
    except urllib2.HTTPError:
        pass

def send_message(conn, device_id):
    params = (('id', device_id), ('lat', random.uniform(59, 61)), ('lon', random.uniform(29, 31)))
    conn.request('GET', '?' + urllib.urlencode(params))
    conn.getresponse().read()

cookie = login()
conn = httplib.HTTPConnection(server)

for i in range(devices):
    device_id = "{0:0>6}".format(i)
    add_device(cookie, device_id)
    send_message(conn, device_id)

while True:
    device_id = "{0:0>6}".format(random.randint(0, devices))
    send_message(conn, device_id)
    time.sleep(1)
