#!/usr/bin/env python3

import urllib
import http.client as httplib
import time
import datetime

id = '12345678901234'
server = 'localhost:5055'

points = [
    ('2025-01-01 00:00:00', 43.34419, -79.83133, 10000000000000.0),
    ('2025-01-01 00:10:00', 43.377530, -79.8320, 50),
    ('2025-01-01 00:10:00', 43.377542, -79.832892, 10000000000000.0),
]

def send(conn, time, lat, lon, speed):
    params = (('id', id), ('timestamp', int(time)), ('lat', lat), ('lon', lon), ('speed', speed))
    conn.request('POST', '?' + urllib.parse.urlencode(params))
    conn.getresponse().read()

conn = httplib.HTTPConnection(server)

for i in range(0, len(points)):
    (moment, lat, lon, speed) = points[i]
#     send(conn, time.mktime(datetime.datetime.strptime(moment, "%Y-%m-%d %H:%M:%S").timetuple()), lat, lon, speed)
    print("sending ", time.mktime(time.localtime(time.time())), lat, lon, speed)
    send(conn, time.mktime(time.localtime(time.time())), lat, lon, speed)
    time.sleep(2)