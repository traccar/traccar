
#
# This test data assumes three devices exist:
#
#     INSERT INTO device ( name, uniqueid ) VALUES
#         ( 'Test Device 1', 123456789012345 ),
#         ( 'Test Device 2', 123456789012 ),
#         ( 'Test Device 3', 123456 );
#
# Note: Not all shells support "echo -e" (e.g., dash, the default on Ubuntu).
#       This will cause an unknown device error for "-e".  bash works.

# testing data:
# ids - 123456789012345 (31), 123456789012 (1), 123456 (2)
# time - 12:00 + X min (where X is protocol number)

# to verify test check database entries

echo "1. gps103"
(echo -n -e "imei:123456789012345,help me,1201011201,,F,120100.000,A,6000.0000,N,13000.0000,E,0.00,;";) | nc -v localhost 5001

echo "2. tk103"
(echo -n -e "(123456789012BP05123456789012345120101A6000.0000N13000.0000E000.0120200000.0000000000L000946BB)";) | nc -v localhost 5002

echo "3. gl100"
(echo -n -e "+RESP:GTSOS,123456789012345,0,0,0,1,0.0,0,0.0,1,130.000000,60.000000,20120101120300,0460,0000,18d8,6141,00,11F0,0102120204\0";) | nc -v localhost 5003

echo "4. gl200"
(echo -n -e "+RESP:GTFRI,020102,123456789012345,,0,0,1,1,0.0,0,0.0,130.000000,60.000000,20120101120400,0460,0000,18d8,6141,00,,20120101120400,11F0\$";) | nc -v localhost 5004

echo "5. t55"
(echo -n -e "\$PGID,123456789012345*0F\r\n\$GPRMC,120500.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,*33\r\n";) | nc -v localhost 5005

echo "6. xexun"
(echo -n -e "111111120009,+436763737552,GPRMC,120600.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,,A*68,F,, imei:123456789012345,04,481.2,F:4.15V,0,139,2689,232,03,2725,0576\n";) | nc -v localhost 5006

echo "7. totem"
(echo -n -e "\$\$B3123456789012345|AA\$GPRMC,120700.000,A,6000.0000,N,13000.0000,E,0.00,,010112,,,A*74|01.8|01.0|01.5|000000000000|20120403234603|14251914|00000000|0012D888|0000|0.0000|3674|940B\r\n";) | nc -v localhost 5007

echo "8. enfora"
(echo -n -e "\x00\x71\x00\x04\x02\x00                 123456789012345 13 \$GPRMC,120800.00,A,6000.000000,N,13000.000000,E,0.0,0.0,010112,,,A*52\r\n";) | nc -v localhost 5008

echo "9. meiligao"
(echo -n -e "\$\$\x00\x60\x12\x34\x56\xFF\xFF\xFF\xFF\x99\x55120900.000,A,6000.0000,N,13000.0000,E,0.00,,010112,,*1C|11.5|194|0000|0000,0000\x69\x62\x0D\x0A";) | nc -v localhost 5009

echo "10. maxon"


echo "11. st210"
(echo -n -e "SA200STT;123456;042;20120101;12:11:00;16d41;-15.618767;-056.083214;000.011;000.00;11;1;41557;12.21;000000;1;3205\r";) | nc -v localhost 5011

echo "12. progress"


echo "13. h02"
(echo -n -e "*HQ,123456789012345,V1,121300,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,ffffffff,000000,000000,000000,000000#";) | nc -v localhost 5013

echo "14. jt600"
(echo -n -e "\$\x00\x00\x12\x34\x56\x11\x00\x1B\x01\x01\x12\x12\x14\x00\x60\x00\x00\x00\x13\x00\x00\x00\x0F\x00\x00\x07\x50\x00\x00\x00\x2B\x91\x04\x4D\x1F\xA1";) | nc -v localhost 5014

echo "15. ev603"
(echo -n -e "!1,123456789012345;!A,01/01/12,12:15:00,60.000000,130.000000,0.0,25101,0;";) | nc -v localhost 5015

echo "16. v680"
(echo -n -e "#123456789012345#1000#0#1000#AUT#1#66830FFB#13000.0000,E,6000.0000,N,001.41,259#010112#121600##";) | nc -v localhost 5016

echo "17. pt502"
(echo -n -e "\$POS,123456,121700.000,A,6000.0000,N,13000.0000,E,0.0,0.0,010112,,,A/00000,00000/0/23895000//\r\n";) | nc -v localhost 5017

echo "18. tr20"
(echo -n -e "%%123456789012345,A,120101121800,N6000.0000E13000.0000,0,000,0,01034802,150,[Message]\r\n";) | nc -v localhost 5018

echo "19. navis"


echo "20. meitrack"
(echo -n -e "\$\$d138,123456789012345,AAA,35,60.000000,130.000000,120101122000,A,7,18,0,0,0,49,3800,24965,510|10|0081|4F4F,0000,000D|0010|0012|0963|0000,,*BF\r\n";) | nc -v localhost 5020

echo "21. skypatrol"


echo "22. gt02"


echo "23. gt06"


echo "24. megastek"
(echo -n -e "STX,102110830074542,\$GPRMC,122400.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,,A*64,F,LowBattery,imei:123456789012345,03,113.1,Battery=24%,,1,460,01,2531,647E;57\r\n";) | nc -v localhost 5024

echo "25. navigil"


echo "26. gpsgate"
(echo -n -e "\$FRLIN,IMEI,123456789012345,*7B\r\n\$GPRMC,122600.000,A,6000.00000,N,13000.00000,E,0.000,0.0,010112,,*0A";) | nc -v localhost 5026

echo "27. teltonika"


echo "28. mta6"


echo "29. mta6can"


echo "30. tlt2h"
(echo -n -e "#123456789012345#V500#0000#AUTO#1\r\n#\$GPRMC,123000.000,A,6000.0000,N,13000.0000,E,0.00,0.00,010112,,,D*70\r\n##";) | nc -v localhost 5030

echo "31. syrus"
(echo -n -e ">REV481669045060+6000000-1300000000000012;ID=123456789012345<";) | nc -v localhost 5031

echo "32. wondex"
(echo -n -e "123456789012345,20120101123200,130.000000,60.000000,0,000,0,0,2\r\n";) | nc -v localhost 5032

echo "33. cellocator"


echo "34. galileo"


echo "35. ywt"
(echo -n -e "%RP,123456789012345:0,120101123500,E130.000000,N60.000000,,0,0,4,0,00\r\n";) | nc -v localhost 5035

echo "36. tk102"
(echo -n -e "[!0000000081r(123456789012345,TK102-W998_01_V1.1.001_130219,255,001,255,001,0,100,100,0,internet,0000,0000,0,0,255,0,4,1,11,00)][=00000000836(ITV123600A6000.0000N13000.0000E000.00001011210010000)]";) | nc -v localhost 5036

echo "37. intellitrac"
(echo -n -e "123456789012345,20120101123700,130.000000,60.000000,0,0,0,7,0,11,15\r\n";) | nc -v localhost 5037

echo "38. xt7"


echo "39. wialon"
(echo -n -e "#L#123456789012345;test\r\n#SD#010112;123900;6000.0000;N;13000.0000;E;0;0;0;4\r\n";) | nc -v localhost 5039

echo "40. carscop"
(echo -n -e "*040331141830UB05123456789012345010112A6000.0000N13000.0000E000.0124000000.0000000000L000000^";) | nc -v localhost 5040

echo "41. apel"


echo "42. manpower"
(echo -n -e "simei:123456789012345,,,tracker,51,24,1.73,120101124200,A,6000.0000,N,13000.0000,E,0.00,28B9,1DED,425,01,1x0x0*0x1*60x+2,en-us,;";) | nc -v localhost 5042

echo "43. globalsat"
(echo -n -e "\$123456789012345,1,1,010112,124300,E13000.0000,N6000.0000,00000,0.0100,147,07,2.4!";) | nc -v localhost 5043

echo "44. atrack"


echo "45. pt3000"
(echo -n -e "%123456789012345,\$GPRMC,124500.000,A,6000.0000,N,13000.0000,E,0.00,,010112,,,A,+100000000000,N098d";) | nc -v localhost 5045

echo "46. ruptela"

echo "47. topflytech"
(echo -n -e "(123456789012345BP00XG00b600000000L00074b54S00000000R0C0F0014000100f0120101124700A6000.0000N13000.0000E000.0000.00)";) | nc -v localhost 5047

echo "48. laipac"
(echo -n -e "\$AVRMC,123456789012345,124800,a,6000.0000,N,13000.0000,E,0.00,0.00,010112,0,3.727,17,1,0,0*17\r\n";) | nc -v localhost 5048

echo "49. aplicom"


echo "50. gotop"
(echo -n -e "#123456789012345,CMD-T,A,DATE:120101,TIME:125000,LAT:60.0000000N,LOT:130.0000000E,Speed:000.0,84-20,000#";) | nc -v localhost 5050
