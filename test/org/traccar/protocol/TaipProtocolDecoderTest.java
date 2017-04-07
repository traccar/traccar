package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class TaipProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        TaipProtocolDecoder decoder = new TaipProtocolDecoder(new TaipProtocol());

        verifyPosition(decoder, text(
                ">REV421942237017+1170957-0701880200000032;ID=356612022463055<"));

        verifyPosition(decoder, text(
                ">RGP200317010815-3852.9306-06204.88560000003000101;&01;ID=5555;#7AD7*51<"));

        verifyPosition(decoder, text(
                ">RCQ09000000000000-3460365-058381460000007F0000000000000115000FFFF1099;#0000;ID=555224;*05<"));

        verifyPosition(decoder, text(
                ">RBR00130217040848-3462200-05846708000175FF0022900003B3C13010800001118410+24061A;ID=555224;*07<"));

        verifyPosition(decoder, text(
                ">REV451891352379+0307152+1016143700000012;SV=8;BL=4416;VO=8055;ID=356612026322000<"));

        verifyPosition(decoder, text(
                ">RGP230615010248-2682523-065236820000003007F4101;ID=0005;#0002;*2A<"),
                position("2015-06-23 01:02:48.000", true, -26.82523, -65.23682));

        verifyPosition(decoder, text(
                ">RGP190805211932-3457215-058493640000000FFBF0300;ID=8251;#2122;*54<"));

        verifyPosition(decoder, text(
                ">RPV00000+3739438-1220384601512612;ID=1234;*7F"));

        verifyPosition(decoder, text(
                "\r\n>REV691615354941+3570173+1397742703203212;ID=Test"));

        verifyPosition(decoder, text(
                ">REV481599462982+2578391-0802945201228512;ID=Test"),
                position("2010-09-02 17:29:42.000", true, 25.78391, -80.29452));
        
        verifyPosition(decoder, text(
                ">REV131756153215+3359479-0075299001031332;VO=10568798;IO=310;SV=10;BL=4190;CV09=0;AD=0;AL=+47;ID=356612021059680"));

        verifyPosition(decoder, text(
                ">RPV02138+4555512-0735478000000032;ID=1005;*76<"));

        verifyPosition(decoder, text(
                ">RPV19105+4538405-0739518900000012;ID=9999;*7A<\r\n"));

    }

}
