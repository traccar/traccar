#!/usr/bin/env python3

import urllib
import http.client as httplib
import time
import datetime

id = '123456789012345'
server = 'localhost:5055'

points = [
    ('2017-01-01 00:00:00', 59.93211887, 30.33050537, 0.0),
    ('2017-01-01 00:05:00', 59.93266715, 30.33190012, 50.0),
    ('2017-01-01 00:10:00', 59.93329069, 30.33333778, 50.0),
    ('2017-01-01 00:15:00', 59.93390346, 30.33468962, 0.0),
    ('2017-01-01 00:20:00', 59.93390346, 30.33468962, 0.0),
    ('2017-01-01 00:25:00', 59.93416146, 30.33580542, 50.0),
    ('2017-01-01 00:30:00', 59.93389271, 30.33790827, 50.0),
    ('2017-01-01 00:35:00', 59.93357020, 30.34033298, 50.0),
    ('2017-01-01 00:40:00', 59.93330144, 30.34252167, 0.0),
    ('2017-01-01 00:44:00', 59.93355945, 30.34413099, 50.0),
    ('2017-01-01 00:50:00', 59.93458072, 30.34458160, 0.0),
    ('2017-01-01 00:55:00', 59.93458072, 30.34458160, 0.0),
]

def send(conn, time, lat, lon, speed):
    params = (('id', id), ('timestamp', int(time)), ('lat', lat), ('lon', lon), ('speed', speed))
    conn.request('POST', '?' + urllib.parse.urlencode(params))
    conn.getresponse().read()

conn = httplib.HTTPConnection(server)

for i in range(0, len(points)):
    (moment, lat, lon, speed) = points[i]
    send(conn, time.mktime(datetime.datetime.strptime(moment, "%Y-%m-%d %H:%M:%S").timetuple()), lat, lon, speed)
