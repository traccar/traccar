package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Pt502ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Pt502ProtocolDecoder decoder = new Pt502ProtocolDecoder(new Pt502Protocol());

        verifyPosition(decoder, text(
                "$POS,865328026243864,151105.000,A,1332.7096,N,204.6787,E,0.0,10.00,050517,,,A/00000,10/1,0/234//FD9/"));

        verifyNull(decoder, text(
                "$FUS865328026243864,510-V1.12,A11-V3.0"));

        verifyPosition(decoder, text(
                "$HDA,20007,134657.000,A,0626.1607,N,00330.2245,E,33.38,81.79,041016,,,A/00010,00000/270,0,0,0/19948900//fa4//"));

        verifyPosition(decoder, text(
                "$HDB,20007,134708.000,A,0626.1759,N,00330.3192,E,26.55,80.37,041016,,,A/00010,00000/23b,0,0,0/19949100//fa4//"));

        verifyPosition(decoder, text(
                "$POS,20007,134704.000,A,0626.1698,N,00330.2870,E,31.23,79.58,041016,,,A/00010,00000/26c,0,0,0/19949100//fa4//#"));

        verifyPosition(decoder, text(
                "$PHO6608,115099,133140.000,A,1307.1238,N,05936.4194,W,0.00,21.50,290816,,,A/00010,00000/0,0,0,0/185100//f59/"));

        verifyPosition(decoder, text(
                "$DFR,40456789,083125.000,A,2232.0971,N,11400.9504,E,0.0,5.00,090714,,,A/00000,00/0,0/200076//FE7/"));

        verifyPosition(decoder, text(
                "$FDA,40456789,083125.000,A,2232.0971,N,11400.9504,E,0.0,5.00,090714,,,A/00000,00/0,0/200076//FE7/"));
        
        verifyPosition(decoder, text(
                "$POS,216769295715,163237.000,A,3258.1738,S,02755.4350,E,0.00,215.88,100915,,,A/0000,0//232300//5b3/"),
                position("2015-09-10 16:32:37.000", true, -32.96956, 27.92392));

        verifyPosition(decoder, text(
                "$POS,11023456,033731.000,A,0335.2617,N,09841.1587,E,0.00,88.12,210615,,,A/0000,0/1f8/388900//f33//"));

        verifyPosition(decoder, text(
                "$POS,6094,205523.000,A,1013.6223,N,06728.4248,W,0.0,99.3,011112,,,A/00000,00000/0/23895000//"));

        verifyPosition(decoder, text(
                "$POS,6120,233326.000,V,0935.1201,N,06914.6933,W,0.00,,151112,,,A/00000,00000/0/0/"));

        verifyPosition(decoder, text(
                "$POS,6002,233257.000,A,0931.0430,N,06912.8707,W,0.05,146.98,141112,,,A/00010,00000/0/5360872"));

        verifyPosition(decoder, text(
                "$POS,6095,233344.000,V,0933.0451,N,06912.3360,W,,,151112,,,N/00000,00000/0/1677600/"));

        verifyPosition(decoder, text(
                "$PHO0,6091,233606.000,A,0902.9855,N,06944.3654,W,0.0,43.8,141112,,,A/00010,00000/0/224000//"));
        
        verifyPosition(decoder, text(
                "$POS,353451000164,082405.000,A,1254.8501,N,10051.6752,E,0.00,237.99,160513,,,A/0000,0/0/55000//a71/"));
        
        verifyPosition(decoder, text(
                "$POS,012896008586486,154215.000,A,0118.0143,S,03646.9144,E,0.00,83.29,180714,,,A/0000,0/0/29200//644/"));
        
        verifyPosition(decoder, text(
                "$POS,1151000,205326.000,A,0901.3037,N,07928.2751,W,48.79,30.55,170814,,,A/00010,10000/0,0,0,0/15986500//fb8/"));

    }

}
