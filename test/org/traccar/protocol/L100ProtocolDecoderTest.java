package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class L100ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        L100ProtocolDecoder decoder = new L100ProtocolDecoder(new L100Protocol());

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
