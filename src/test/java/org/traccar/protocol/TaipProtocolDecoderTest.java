package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class TaipProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new TaipProtocolDecoder(null);

        verifyAttribute(decoder, text(
                ">RUS00,111220124402-3138067-06417623000012200FF,000000000000000000000000000,0000000111,15640422,00000,+25.5,00000,51;ID=CST3G0443;#IP1:089F;*34<"),
                Position.PREFIX_TEMP + 1, 25.5);

        verifyAttribute(decoder, text(
                ">RUS00,031120185945-3138060-06417622000209200FF,000000000000000000000000000,0000000000,11440419,00000,00000,00000,00;ID=CST3G0495;#IP0:1EF7;*4B<"),
                Position.KEY_BATTERY, 4.19);

        verifyPosition(decoder, text(
                ">RUS00,031120185945-3138060-06417622000209200FF,000000000000000000000000000,0000000000,11440419,00000,00;ID=CST3G0495;#IP0:1EF7;*4B<"));

        verifyPosition(decoder, text(
                ">RGP041120190000-3137454-064075520001883004D50;ID=8385;#IP0:0080;*19<"));

        verifyNull(decoder, text(
                ">RLN25601000+297185103-0955755990+000059150000+0000000012000000000000000000000000000000000000000000000000000000000012;ID=3580;*48<"));

        verifyPosition(decoder, text(
                ">RGP211217112154-2748332-058946350000000FF7F2100;ID=AA01;#0002;*2D<"));

        verifyPosition(decoder, text(
                ">RCV12270218010247-3471349-058400030002057F001200020A1D013010600001509+0000FF+0000FF;#1DE2;ID=7196;*03<"));

        verifyPosition(decoder, text(
                ">RPV03874+3477708-0923453100029212;ID=0017;*71<"));

        verifyNull(decoder, text(
                ">RAL03874+00185+00012;ID=0017;*4A<"));

        verifyNull(decoder, text(
                ">RCP03874+347771-092345312;ID=0017;*65<"));

        verifyNull(decoder, text(
                ">RLN03874000+347770828-0923453071+000608270000+0000292309000000000000000000000000000000000000000000000012;ID=0017;*49<"));

        verifyPosition(decoder, text(
                ">RPV46640+4197412-0752857900015802;ID=5102;*71<"));

        verifyNull(decoder, text(
                ">RCP46640+419741-075285802;ID=5102;*6C<"));

        verifyPosition(decoder, text(
                ">REV001958003965+0307178+1016144900031532;IO=300;SV=8;BL=4159;CF=8161,C,13;AD=14145;IX=10233040;FF=0,0,0,0;VO=338578;ID=357042063052352<"));

        verifyPosition(decoder, text(
                ">REV011958000369+0307185+1016144400000032;IO=200;SV=9;BL=4158;CF=0,0,0;AD=12347;IX=10213040;FF=0,0,0,0;VO=338572;ID=357042063052352<"));

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
