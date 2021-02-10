#!/usr/bin/python

import socket
import binascii
import time, sys

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
port = int(sys.argv[1])
s.connect(("localhost", port))
# s.send(binascii.unhexlify('68680f0504035889905831401700df1a00000d0a'))
# str = "imei:869867038141406,tracker,151030080103,,F,000101.000,A,1513.1834,N,1356.9071,E,0.00,0;"
# str = "$HDR,2020A,3.2.37,NR,01,L,869867038141406,VEH_REG_NO,1,30012021,044928,28.647620,N,77.193142,E,0.0,328.63,11,239.6,1.63,0.84,Vodafone,0,1,24.5,3.9,0,O,31,404,11,428,2fb8,428,ffa2,31,428,1c49,31,428,2852,30,428,3e05,30,0000,00,541,3164*"
# str = "$HDR,2025A,4.0.3,NR,01,L,869867038064665,89910426011715133333,O,1,05022021,093445,28.647619,N,77.193138,E,000.00,106.87,15,238.3,Vodafone,31,0,0,0,1,24.6,4.1,0.0,XXX.X,X1,X2,X3,134,268E*"
str =   "$HDR,2020A,3.2.37,NR,01,L,869867038160968,VEH_REG_NO,1,05022021,215441,28.6476271,N,77.193158,E,0.0,    246.49,15,226.5,  1.18,0.65,  Vodafone,0,1,24.6,3.9,0,O,31,404,11,428,2fb8,428,1c4b,xx,428,ffa2,29,428,2fba,28,428,2852,27,0000,00,841,32A5*"
# str =   "$HDR,2020A,3.2.37,NR,01,L,869867038160968,VEH_REG_NO,1,08022021,105118,028.647621,N,077.193138,E,000.00,000.00,17,0230.08,01,01,Vodafone,1,1,20.0,3.9,0,O,31,404,11,428,2fb8,428,1c4a,26,428,5b83,25,428,5b84,25,428,2852,24,1110,00,235,31B0*"
str =   "$2030,M2CD,3.3.37,NR,01,L,869867038242493,VEH_REG_NO,1,08022021,110701,028.647631,N,077.193130,E,000.00,000.00,18,0230.21,01,01,Vodafone,1,1,19.9,3.9,0,O,31,404,11,428,2fb8,428,1c4b,28,428,5b84,27,428,5b83,26,428,2852,24,1111,00,325,31A0*"
# $AD,869867038242493,0,000000,000000,00.000000,0,00.000000,0,000.00,x,X,X,X,ECE*
str="$HDR,ADTI,3.3.37,NR,01,L,869867038160968,VEH_REG_NO,1,08022021,131345,028.647680,N,077.192932,E,0.0,244.60,06,0214.77,06,05,Vodafone,0,0,0.0,3.5,0,O,31,404,11,428,2fb8,428,1c4b,xx,428,2852,29,428,5b84,28,428,1c4a,27,0000,00,906,31E8*"
str = "#M2C,2020,P1.B1.H3.F9.R3,102,869867038160968,1,H,0,0,190427,181151,19.112085,72.850685,0,275,0.0,0,0,195,255,3327,3479,5920,0,0.0, 0,0,0,404,20,4683,2923,11,0*7976"
str = "[#M2C,2020,P1.B1.H3.F9.R3,102,869867038160968,1,H,0,0,190427,181151,19.112085,72.850685,0,275,0.0,0,0,195,255,3327,3479,5920,0,0.0,0,0,0,404,20,4683,2923,11,0*7976\r\n#M2C,2020,P1.B1.H3.F9.R3,102,869867038160968,2,H,0,20,190427,181158,19.112085,72.850685,0,275,0.0,0,0,67,255,8876,3479,5920,0,0.0,0,0,0,404,20,4683,2923,11,0*7998]"
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
