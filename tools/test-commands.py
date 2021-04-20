#!/usr/bin/python

import socket
import binascii
import time, sys
from datetime import date, datetime
import pytz

# it will get the time zone 
# of the specified location
UTC = pytz.timezone('UTC')

# dd/mm/YY H:M:S
now = datetime.now(UTC)

dt_string = now.strftime("%d%m%Y,%H%M%S")
print("date and time =", dt_string)


s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
port = int(sys.argv[1])
host = (sys.argv[2])
# host = "localhost"
# host = "13.127.235.205"
print(host + "***" + str(port))
s.connect((host, port))
# s.send(binascii.unhexlify('68680f0504035889905831401700df1a00000d0a'))
# str = "imei:869867038141406,tracker,151030080103,,F,000101.000,A,1513.1834,N,1356.9071,E,0.00,0;"
# str = "#M2C,2020,P1.B1.H3.F9.R3,102,869867038160968,1,H,0,0,190427,181151,19.112085,72.850685,0,275,0.0,0,0,195,255,3327,3479,5920,0,0.0, 0,0,0,404,20,4683,2923,11,0*7976" 
# str="2030,M2CD,3.3.37,NR,02,H,869867038190056,VEH_REG_NO,1,{},028.647776,N,077.193100,E,0.0000,000.00,08,0221.11,01,02,Vodafone,0,1,8.8,3.9,0,C,31,404,11,428,1c4a,428,1c4b,xx,428,1c49,xx,428,5638,xx,428,2fb8,xx,0000,00,31632,3382*"
# str="$HDR,2025A,4.0.12,NR,01,L,1234,89911190185451856414,C,1,{},28.647755,N,77.193092,E,100.00,117.99,18,227.6,Vodafone,31,0,0,1,1,12.9,4.1,0.1,23.33,X1,X2,X3,5939,2715*"
# str="2030,M2CD,3.3.37,NR,01,L,1234,VEH_REG_NO,1,17042021,100301,028.647781,N,077.193100,E,000.00,000.00,10,0227.35,01,02,Vodafone,1,1,8.8,3.9,1,C,31,404,11,428,1c4a,428,1c4b,10,428,1c49,00,428,2fb8,00,428,a4e9,00,1000,00,72911,346F*"
str=  "$HDR,2025A,4.0.12,NR,01,L,869867038227189,89911190185451856414,O,0,000000,000000,00.000000,0,00.000000,0,00.00,00.00,00,000.00,Vodafone,31,0,0,0,1,9.1,4.1,0.0,XXX.X,X1,X2,X3,5939,2726*"
str = str.format(dt_string)

s.send(str)
#1313.1834,N,1956.9071,E 13.219723  19.948452


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
