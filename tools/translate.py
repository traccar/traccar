#!/usr/bin/python

import os
import optparse
import urllib2
import json
import base64

parser = optparse.OptionParser()
parser.add_option("-u", "--user", dest="username", help="transifex user login")
parser.add_option("-p", "--password", dest="password", help="transifex user password")

(options, args) = parser.parse_args()

if not options.username or not options.password:
    parser.error('User name and password are required')

os.chdir(os.path.dirname(os.path.abspath(__file__)))

path = "../web/l10n/"

def request(url):
    req = urllib2.Request(url)
    auth = base64.encodestring("%s:%s" % (options.username, options.password)).replace("\n", "")
    req.add_header("Authorization", "Basic %s" % auth)
    return urllib2.urlopen(req)

resource = json.load(request("https://www.transifex.com/api/2/project/traccar/resource/web/?details"))

for language in resource["available_languages"]:
    code = language["code"]
    data = request("https://www.transifex.com/api/2/project/traccar/resource/web/translation/" + code + "?file")
    file = open(path + code + ".json", "wb")
    file.write(data.read())
    file.close()
