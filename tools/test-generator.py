#!/usr/bin/python

import sys
import math
import urllib
import urllib2
import time

id = '123456789012345'
server = 'http://localhost:5055'
period = 1
step = 0.001

waypoints = [
    (40.722412, -74.006288),
    (40.728592, -74.005258),
    (40.728348, -74.002822),
    (40.725437, -73.996750),
    (40.721778, -73.999818),
    (40.723323, -74.002994)
]

points = []

for i in range(0, len(waypoints)):
    (lat1, lon1) = waypoints[i]
    (lat2, lon2) = waypoints[(i + 1) % len(waypoints)]
    length = math.sqrt((lat2 - lat1) ** 2 + (lon2 - lon1) ** 2)
    count = int(math.ceil(length / step))
    for j in range(0, count):
        lat = lat1 + (lat2 - lat1) * j / count
        lon = lon1 + (lon2 - lon1) * j / count
        points.append((lat, lon))

def send(lat, lon, course):
    params = (('id', id), ('timestamp', int(time.time())), ('lat', lat), ('lon', lon), ('bearing', course))
    urllib2.urlopen(server + '?' + urllib.urlencode(params)).read()

def course(lat1, lon1, lat2, lon2):
    lat1 = lat1 * math.pi / 180
    lon1 = lon1 * math.pi / 180
    lat2 = lat2 * math.pi / 180
    lon2 = lon2 * math.pi / 180
    y = math.sin(lon2 - lon1) * math.cos(lat2)
    x = math.cos(lat1) * math.sin(lat2) - math.sin(lat1) * math.cos(lat2) * math.cos(lon2 - lon1)
    return (math.atan2(y, x) % (2 * math.pi)) * 180 / math.pi

index = 0

while True:
    (lat1, lon1) = points[index % len(points)]
    (lat2, lon2) = points[(index + 1) % len(points)]
    send(lat1, lon1, course(lat1, lon1, lat2, lon2))
    time.sleep(period)
    index += 1
