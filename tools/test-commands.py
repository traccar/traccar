#!/usr/bin/python

import socket
import binascii
import time, sys

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
port = int(sys.argv[1])
host = "localhost"
# host = "15.206.203.92"
print(host + "***" + str(port))
s.connect((host, port))
# s.send(binascii.unhexlify('68680f0504035889905831401700df1a00000d0a'))
# str = "imei:869867038141406,tracker,151030080103,,F,000101.000,A,1513.1834,N,1356.9071,E,0.00,0;"
# str = "$HDR,2020A,3.2.37,NR,01,L,869867038141406,VEH_REG_NO,1,30012021,044928,28.647620,N,77.193142,E,0.0,328.63,11,239.6,1.63,0.84,Vodafone,0,1,24.5,3.9,0,O,31,404,11,428,2fb8,428,ffa2,31,428,1c49,31,428,2852,30,428,3e05,30,0000,00,541,3164*"
# str =   "$HDR,2020A,3.2.37,NR,01,L,860697053338069,VEH_REG_NO,1,05042021,215441,28.6476271,N,77.193158,E,0.0,246.49,15,226.5,1.18,0.65,Vodafone,0,1,24.6,3.9,0,O,31,404,11,428,2fb8,428,1c4b,xx,428,ffa2,29,428,2fba,28,428,2852,27,0000,00,841,32A5*"
# str =   "$HDR,2020A,3.2.37,NR,01,L,869867038160968,VEH_REG_NO,1,05042021,105118,028.647621,N,077.193138,E,000.00,000.00,17,0230.08,01,01,Vodafone,1,1,20.0,3.9,0,O,31,404,11,428,2fb8,428,1c4a,26,428,5b83,25,428,5b84,25,428,2852,24,1110,00,235,31B0*"
# s.send(binascii.unhexlify('68680f0504035889905831401700df1a00000d0a'))
# str = "[#M2C,2020,P1.B1.H3.F9.R3,102,869867038160968,1,H,0,0,190427,181151,19.112085,72.850685,0,275,0.0,0,0,195,255,3327,3479,5920,0,0.0,0,0,0,404,20,4683,2923,11,0*7976\r\n#M2C,2020,P1.B1.H3.F9.R3,102,869867038160968,2,H,0,20,190427,181158,19.112085,72.850685,0,275,0.0,0,0,67,255,8876,3479,5920,0,0.0,0,0,0,404,20,4683,2923,11,0*7998]"
# str = "(084122341666BR00210212A2838.8581N07711.5886E000.0082028000.0001000004L01F67178)";
# $AD,869867038242493,0,000000,000000,00.000000,0,00.000000,0,000.00,x,X,X,X,ECE*
# str =   "$2030,M2CD,3.3.37,NR,01,L,869867038242493,VEH_REG_NO,1,05042021,110701,028.647631,N,077.193130,E,000.00,000.00,18,0230.21,01,01,Vodafone,1,1,19.9,3.9,0,O,31,404,11,428,2fb8,428,1c4b,28,428,5b84,27,428,5b83,26,428,2852,24,1111,00,325,31A0*"
# str = "imei:869867038141406,tracker,151030080103,,F,000101.000,A,1513.1834,N,1356.9071,E,0.00,0;"
# str = "#M2C,2020,P1.B1.H3.F9.R3,102,869867038160968,1,H,0,0,190427,181151,19.112085,72.850685,0,275,0.0,0,0,195,255,3327,3479,5920,0,0.0, 0,0,0,404,20,4683,2923,11,0*7976"
# str="$HDR,ADTI,3.3.37,NR,01,L,869867038160968,VEH_REG_NO,1,05042021,131345,028.647680,N,077.192932,E,0.0,244.60,06,0214.77,06,05,Vodafone,0,0,0.0,3.5,0,O,31,404,11,428,2fb8,428,1c4b,xx,428,2852,29,428,5b84,28,428,1c4a,27,0000,00,906,31E8*"
str=  "$HDR,2025,3.1.37,IN,07,L,869867038065258,VEH_REG_NO,1,05042021,082702,28.691307,N,77.198630,E,0.0,109.18,10,161.5,1.73,1.45,Vodafone,1,1,12.4,4.1,0,O,28,404,11,420,9b16,420,87dd,22,420,9b17,21,401,5fdd,19,420,87df,18,1000,00,1826,31B2*"
str = "$HDR,2025A,4.0.03,NR,01,L,869867038141406,89910426011715133333,O,1,05042021,093445,28.447619,N,77.193138,E,000.00,106.87,15,238.3,Vodafone,31,0,0,0,1,24.6,4.1,0.0,XXX.X,X1,X2,X3,134,268E*"
str=  "$HDR,2025A,4.0.12,NR,01,L,869867038190056,89910483091800268194,O,1,05042021,085919,28.647652,N,77.193108,E,000.00,220.48,13,247.8,Vodafone,31,1,1,0,1,20.2,4.1,0.1,XXX.X,X1,X2,X3,1698,270F*"
# str = "$HDR,2020A,3.2.37,NR,01,L,869867038141406,VEH_REG_NO,1,30012021,044928,28.647620,N,77.193142,E,0.0,328.63,11,239.6,1.63,0.84,Vodafone,0,1,24.5,3.9,0,O,31,404,11,428,2fb8,428,ffa2,31,428,1c49,31,428,2852,30,428,3e05,30,0000,00,541,3164*"
# str = "[#M2C,2020,P1.B1.H3.F9.R3,102,869867038160968,1,H,0,0,190427,181151,19.112085,72.850685,0,275,0.0,0,0,195,255,3327,3479,5920,0,0.0,0,0,0,404,20,4683,2923,11,0*7976\r\n#M2C,2020,P1.B1.H3.F9.R3,102,869867038160968,2,H,0,20,190427,181158,19.112085,72.850685,0,275,0.0,0,0,67,255,8876,3479,5920,0,0.0,0,0,0,404,20,4683,2923,11,0*7998]"
# s.sendall(str.encode('utf-8'))
# str="(084122341666BP05000084122341666210331A2838.8759N07711.5667E000.1114032000.0001000004L01F67178)"
# str ="$HDR,2025,3.1.37,NR,01,L,869867038065258,VEH_REG_NO,1,05042021,145119,28.647697,N,77.193140,E,010.00,80.02,16,226.3,1.02,0.66,Vodafone,1,1,20.1,4.1,0,O,31,404,11,428,2fb8,428,2852,30,428,a4e9,30,428,1c4a,29,428,1c4b,28,1100,00,25,3179*"
# str="[#M2C,2020,P1.B1.H1.F1.R1,101,869867038065258,2,L,1,100,170704,074933,28.647556,77.192940,900,194,0.0,0,0,0,255,11942,0,0,0,0,0,0,0,0,30068,5051,0,0,1*8159\r\n";
# str="2030,M2CD,3.3.37,NR,01,L,869867038190056,VEH_REG_NO,1,06042021,183003,028.647753,N,077.193092,E,000.40,000.00,08,0227.44,02,01,Vodafone,1,1,8.8,3.9,0,C,31,404,11,428,1c4a,428,1c4b,xx,428,1c49,xx,428,2852,xx,428,5638,29,1000,00,11608,3344*"

#2020a : ignition on , why offline if still same
str="$HDR,2020A,3.2.37,NR,01,L,869867038160364,VEH_REG_NO,1,11042021,041652,28.647728,N,77.193122,E,000.00,212.07,17,244.9,0.97,0.62,Vodafone,1,1,12.8,3.8,0,C,31,404,11,428,1c4b,428,1c4a,xx,428,1c49,xx,428,a4e9,xx,428,2852,xx,1111,00,48870,3488*"
str="$HDR,2020A,3.2.37,NR,01,L,869867038160364,VEH_REG_NO,1,11042021,041642,28.647732,N,77.193127,E,000.40,46.880,16,245.1,1.00,0.63,Vodafone,1,1,12.8,3.8,0,C,31,404,11,428,1c4b,428,1c4a,xx,428,1c49,xx,428,a4e9,xx,428,2852,31,1111,00,48869,33CF*"
str="$HDR,2020A,3.2.37,NR,01,L,869867038160364,VEH_REG_NO,1,11042021,041632,28.647728,N,77.193125,E,000.73,10.64,16,245.1,1.00,0.63,Vodafone,1,1,12.8,3.8,0,C,31,404,11,428,1c4b,428,1c4a,xx,428,1c49,xx,428,a4e9,xx,428,2852,xx,1111,00,48868,3453*"

#2030 1 min ignition off but state does not show false
str="2030,M2CD,3.3.37,NR,02,H,869867038190056,VEH_REG_NO,1,09042021,130902,028.647776,N,077.193100,E,0.0,000.00,08,0221.11,01,02,Vodafone,0,1,8.8,3.9,0,C,31,404,11,428,1c4a,428,1c4b,xx,428,1c49,xx,428,5638,xx,428,2fb8,xx,0000,00,31632,3382*"

#2026 all issues off
# str="$HDR,2025A,4.0.12,NR,01,L,869867038227189,89911190185451856414,O,1,11042021,041332,28.647755,N,77.193092,E,000.00,117.99,18,227.6,Vodafone,31,1,1,0,1,12.9,4.1,0.1,XXX.X,X1,X2,X3,5939,2715*"

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
