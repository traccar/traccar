package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class EasyTrackProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        EasyTrackProtocolDecoder decoder = new EasyTrackProtocolDecoder(new EasyTrackProtocol());

        verifyPosition(decoder, text(
                "*ET,358155100054249,HB,A,100b06,053318,803a0b51,03d507c9,0017,0000,00400000,07,100,0000,1435,63"));

        verifyNothing(decoder, text(
                "*ET,358155100054249,MQ"));

        verifyNothing(decoder, text(
                "*ET,358155100054249,TX,A,100b06,053230"));

        verifyPosition(decoder, text(
                "*ET,358155100054249,HB,A,100b06,053212,803a0b20,03d507a2,0054,0000,40400000,06,100,0000,1435,44"));

        verifyNothing(decoder, text(
                "*ET,135790246811221,GZ,0001,0005"));

        verifyPosition(decoder, text(
                "*ET,135790246811221,DW,A,0A090D,101C0D,00CF27C6,0413FA4E,0000,0000,00000000,20,4,0000,00F123"),
                position("2010-09-13 16:28:13.000", true, 22.62689, 114.03021));

        verifyNothing(decoder, text(
                "*ET,358155100048430,CC,0.0,V,100603,141817,80d77ae8,81ab1ffd,0000,6b08,40000000,19,99,0000,fa9,918"));

        verifyPosition(decoder, text(
                "*ET,135790246811221,DW,A,050915,0C2A27,00CE5954,04132263,0000,0000,01000000,20,4,0000,001254"));

        verifyPosition(decoder, text(
                "*ET,135790246811221,DW,A,0A090D,101C0D,00CF27C6,0413FA4E,0000,0000,00000000,20,4,0000,00F123,100"));

        verifyPosition(decoder, text(
                "*ET,135790246811221,DW,A,0A090D,101C0D,00CF27C6,8413FA4E,0000,0000,00000000,20,4,0000,00F123,100"));

        verifyPosition(decoder, text(
                "*ET,358155100003016,HB,A,0d081e,07381e,8038ee09,03d2e9be,004f,0000,40c00000,0f,100,0000,00037c,29"));

        verifyPosition(decoder, text(
                "*ET,358155100003016,HB,A,0d081e,073900,8038ee2f,03d2e9fd,0114,0000,40c00000,12,100,0000,00037c,32"));

        verifyPosition(decoder, text(
                "*ET,135790246811221,HB,A,050915,0C2A27,00CE5954,04132263,0000,0000,01000000,20,4,0000,00F123,100,200"));

    }

}
