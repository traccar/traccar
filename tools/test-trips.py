#!/usr/bin/env python3

import urllib
import http.client as httplib
import time
import datetime

id = '123401234'
server = 'localhost:5055'

points = [
    ('2025-01-29 12:00:00', 43.34419, -79.83133, 45),
    ('2025-01-29 12:10:00', 43.37753, -79.83200, 50),
    ('2025-01-29 12:20:00', 43.37754, -79.83289, 65),
    ('2025-01-29 12:30:00', 43.38000, -79.83300, 40),
    ('2025-01-29 12:40:00', 43.38500, -79.83400, 55),
    ('2025-01-29 12:50:00', 43.39000, -79.83500, 70),
    ('2025-01-29 13:00:00', 43.39500, -79.83600, 45),
    ('2025-01-29 13:10:00', 43.40000, -79.83700, 60),
    ('2025-01-29 13:20:00', 43.40500, -79.83800, 75),
    ('2025-01-29 13:30:00', 43.41000, -79.83900, 50),
    ('2025-01-29 13:40:00', 43.41500, -79.84000, 65),
    ('2025-01-29 13:50:00', 43.42000, -79.84100, 80),
    ('2025-01-29 14:00:00', 43.42500, -79.84200, 45),
    ('2025-01-29 14:10:00', 43.43000, -79.84300, 55),
    ('2025-01-29 14:20:00', 43.43500, -79.84400, 70),
]

def send(conn, time, lat, lon, speed):
    params = (
        ('id', id),
        ('timestamp', int(time)),
        ('lat', lat),
        ('lon', lon),
        ('speed', speed),
    )
    conn.request('POST', '?' + urllib.parse.urlencode(params))
    conn.getresponse().read()

conn = httplib.HTTPConnection(server)

for i in range(0, len(points)):
    (moment, lat, lon, speed) = points[i]
    current_time = time.mktime(time.localtime(time.time()))
    print(f"sending point {i+1}/{len(points)}: ", current_time, lat, lon, speed)
    send(conn, current_time, lat, lon, speed)
    time.sleep(2)