package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class ArnaviTextProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new ArnaviTextProtocolDecoder(null));

        verifyPosition(decoder, buffer(
                "$AV,V4,999999,12487,2277,203,65534,0,0,193,65535,65535,65535,65535,1,13,80.0,56.1,200741,5950.6773N,03029.1043E,300.0,360.0,121012,65535,65535,65535,SF*6E"));

        verifyNull(decoder, buffer(
                "$AV,V3DI,85164,20707,-1,19,0008C56A,000879AC,0C000002,863071013041618,89997077111301204297,*0B"));

        verifyNull(decoder, buffer(
                "$AV,V6SD,85164,20708,-1,3,6,37,33,*52"));

        verifyAttributes(decoder, buffer(
                "$AV,V4,85164,20709,1148,418,-1,0,1,192,0,0,0,0,0,0,,,000023,0000.0000N,00000.0000E,0.0,0.0,060180,0,0,32767,*4F"));

        verifyNull(decoder, buffer(
                "$AV,V3GSMINFO,85164,-1,20450,KMOBILE,1,2,1,23,0,40101,cc3,1,ce19,401,16,65304,5613,72,,SF*7F"));

        verifyAttributes(decoder, buffer(
                "$AV,V4,85164,20451,1146,418,-1,1,1,192,0,0,0,0,0,0,,,104340,0000.0000N,00000.0000E,0.0,0.0,060219,11,0,32767,,SF*47"));

        verifyNull(decoder, buffer(
                "$AV,V6SD,85164,20452,-1,3,3,5,6769,,SF*5D"));

        verifyPosition(decoder, buffer(
                "$AV,V2,32768,12487,2277,203,-1,0,0,193,0,0,1,13,200741,5950.6773N,03029.1043E,0.0,0.0,121012,*6E"));

        verifyPosition(decoder, buffer(
                "$AV,V3,999999,12487,2277,203,65534,0,0,193,65535,65535,65535,65535,1,13,200741,5950.6773N,03029.1043E,300.0,360.0,121012,65535,65535,65535,SF*6E"));

    }

}
