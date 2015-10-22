package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

public class EasyTrackProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        EasyTrackProtocolDecoder decoder = new EasyTrackProtocolDecoder(new EasyTrackProtocol());

        verifyNothing(decoder, text(
                "*ET,135790246811221,GZ,0001,0005"));

        verifyPosition(decoder, text(
                "*ET,135790246811221,DW,A,0A090D,101C0D,00CF27C6,0413FA4E,0000,0000,00000000,20,4,0000,00F123"));

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
