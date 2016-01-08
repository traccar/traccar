#!/usr/bin/python

import sys
import re
import socket
import binascii

if len(sys.argv) < 2:
    sys.exit("log file is not provided")

path = sys.argv[1]
p = re.compile(r"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} DEBUG: \[([0-9a-fA-F]{8}): (\d+) < [\d.]+] HEX: ([0-9a-fA-F]+)")

ports = {}
messages = {}

for line in open(path):
    if "HEX:" in line:
        m = p.match(line)
        if m:
            session = m.group(1)
            port = m.group(2)
            message = m.group(3)
            ports[session] = port
            if session not in messages:
                messages[session] = []
            messages[session].append(message)

for session in ports:
    port = ports[session]
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect(("localhost", int(port)))
    for message in messages[session]:
        s.send(binascii.unhexlify(message))
    s.close()
