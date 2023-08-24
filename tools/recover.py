#!/usr/bin/env python3

import sys
import re
import os
import xml.etree.ElementTree
import socket
import binascii
import time

if len(sys.argv) < 2:
    sys.exit("log file is not provided")

path = sys.argv[1]
p = re.compile(r"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}  INFO: \[([TU][0-9a-fA-F]{8}): (\S+) < [\d.]+] ([0-9a-fA-F]+)")

def load_ports():
    ports = {}
    dir = os.path.dirname(os.path.abspath(__file__))
    root = xml.etree.ElementTree.parse(dir + '/../setup/default.xml').getroot()
    for entry in root.findall('entry'):
        key = entry.attrib['key']
        if key.endswith('.port'):
            ports[key[:-5]] = int(entry.text)
    return ports

ports = load_ports()
protocols = {}
messages = {}

for line in open(path):
    print(line)
    m = p.match(line)
    if m:
        session = m.group(1)
        protocol = m.group(2)
        message = m.group(3)
        protocols[session] = protocol
        if session not in messages:
            messages[session] = []
        messages[session].append(message)

print('Total: %d' % len(messages))

for session in protocols:
    port = ports[protocols[session]]
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect(("localhost", int(port)))
    for message in messages[session]:
        s.send(binascii.unhexlify(message))
        time.sleep(0.1)
    s.close()
