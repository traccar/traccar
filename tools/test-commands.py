#!/usr/bin/env python3

import socket
import binascii

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect(("localhost", 5001))
#s.send(binascii.unhexlify('68680f0504035889905831401700df1a00000d0a'))
s.send("imei:123456789012345,tracker,151030080103,,F,000101.000,A,5443.3834,N,02512.9071,E,0.00,0;")

while True:
    print s.recv(1024)

s.close()
