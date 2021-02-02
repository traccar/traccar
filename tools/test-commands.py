#!/usr/bin/python

import socket
import binascii
import time, sys

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
port = int(sys.argv[1])
s.connect(("localhost", port))
# s.send(binascii.unhexlify('68680f0504035889905831401700df1a00000d0a'))
# str = "imei:869867038141406,tracker,151030080103,,F,000101.000,A,1513.1834,N,1356.9071,E,0.00,0;"
str = "$HDR,2020A,3.2.37,NR,01,L,869867038141406,VEH_REG_NO,1,30012021,044928,28.647620,N,77.193142,E,0.0,328.63,11,239.6,1.63,0.84,Vodafone,0,1,24.5,3.9,0,O,31,404,11,428,2fb8,428,ffa2,31,428,1c49,31,428,2852,30,428,3e05,30,0000,00,541,3164*"
# s.sendall(str.encode('utf-8'))
s.send(str)
#1313.1834,N,1956.9071,E 13.219723  19.948452

# while True:
#     print ('iiii')
#     print (s.recv(4096))
def recv_timeout(the_socket,timeout=2):
    #make socket non blocking
    the_socket.setblocking(0)
    
    #total data partwise in an array
    total_data=[];
    data='';
    
    #beginning time
    begin=time.time()
    while 1:
        #if you got some data, then break after timeout
        if total_data and time.time()-begin > timeout:
            break
        
        #if you got no data at all, wait a little longer, twice the timeout
        elif time.time()-begin > timeout*2:
            break
        
        #recv something
        try:
            data = the_socket.recv(8192)
            if data:
                total_data.append(data)
                #change the beginning time for measurement
                begin=time.time()
            else:
                #sleep for sometime to indicate a gap
                time.sleep(0.1)
        except:
            pass
    
    #join all parts to make final string
    return ''.join(total_data)

#get reply and print
print recv_timeout(s)
print ('iiiiii6666')
s.close()
