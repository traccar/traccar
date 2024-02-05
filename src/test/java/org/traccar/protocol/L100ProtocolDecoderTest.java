package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class L100ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new L100ProtocolDecoder(null));

        verifyPosition(decoder, text(
                "ATL,NP,868004029750174,$GPRMC,062943,A,2533.6719,N,09154.3203,E,0,179,311218,,,*39,#01111011000000,0,0,0,934.82,27.13,4.0,25,405,755,15af,974b,0,0,0,ATL"));

        verifyPosition(decoder, text(
                "ATL,L,868345038171963,N,201218,093016,A,025.067144,N,055.144833,E,000.0,GPS,3939,424,03,00405,008834"));

        verifyPosition(decoder, text(
                "N,111116,090031,A,028.123456,N,077.123456,E,000.0,GPS,4180,404,11,00159,064753"));

        verifyAttributes(decoder, text(
                "L,ATLOBD,866795030475584,03,7429,143344,130918,CAN,0101:00076100,0103:0200,0104:3C,0105:84,010A:XX,010B:19,010C:0F98,010D:22,010E:68,010F:5A,0110:XXXX,0111:28,011C:20,011F:XXXX,0121:0000,0122:XXXX,012F:XX,0162:XX,0132:XXXX,0133:61,0143:00A8,0145:0F,0146:XX,0147:30,0148:XX,0149:31,014A:18,014B:XX,014C:92,0151:XX,0131:00BB,0144:8000,015E:XXXX,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,0902:XXXXXXXXXXXXXXX"));

        verifyPosition(decoder, text(
                "H,ATL,866795030478513,02,0981,054448,230318,A,28.633486;N,77.222595;E,0,154,1.14,4.2,18,404,4,88,ad7b,#1031,0,ATL,"));

        verifyNull(decoder, text(
                "L,ATL,866795030477952,01,0035,"));

        verifyPosition(decoder, text(
                "ATL861693039769518,$GPRMC,074930.000,A,2838.0112,N,07713.3602,E,0000,223.36,290518,,,A*7E,#01111011000100,0.012689,0,0,2.572415,0,4.015,22,404,4,88,3ad5,0,0.01,1.4_800F_VTS3D3_gen_peri_myn,,internet,00000000,ATL"));

        verifyPosition(decoder, text(
                "ATL867857039216564,$GPRMC,131101,A,2838.010010,N,7713.354980,E,0,0,240418,,,*09,#00011011000000,0,0,0,10.70,24.31,3.8,0,0,0,0,0ATL"));

        verifyPosition(decoder, text(
                "ATL867857039216564,$GPRMC,131033,A,2838.010010,N,7713.354980,E,0,51,240418,,,*3D,#00011011000000,0,0,0,10.70,24.31,3.8,20,404,4,88,cfaaATL"));

        verifyPosition(decoder, text(
                "ATL868004026997257,$GPRMC,095542,A,2838.0107,N,07713.3579,E,0,98,010617,,,*03,#01111011000000,0,0,0,0.01,45.94,4.0,25,404,4,88,3ad5ATL"));

        verifyPosition(decoder, text(
                "ATL861693035285253,$GPRMC,022040,A,2954.0481,N,07353.1694,E,0,150,280417,,,*36,#01111011000000,0,0,0,82.92,37.92,4.0,23,404,70,163,b178ATL"));

        verifyPosition(decoder, text(
                "ATL356895037533745,$GPRMC,111719.000,A,2838.0045,N,07713.3707,E,0.00,,120810,,,A*75,#01100111001010,N.C,N.C,N.C,12345.67,31.4,4.2,21,100,000,000001,00000ATL"));

    }

}
