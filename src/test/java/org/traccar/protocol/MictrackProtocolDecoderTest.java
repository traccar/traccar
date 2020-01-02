package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class MictrackProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        MictrackProtocolDecoder decoder = new MictrackProtocolDecoder(null);

        verifyPosition(decoder, text(
                "MT;6;866425031361423;R0;10+190109091803+22.63827+114.02922+2.14+69+2+3744+113"),
                position("2019-01-09 09:18:03.000", true, 22.63827, 114.02922));

        verifyAttributes(decoder, text(
                "MT;6;866425031377981;R1;190108024848+6a:db:54:5a:79:6d,-91,00:9a:cd:a2:e6:21,-94+3+3831+0"));

        verifyAttributes(decoder, text(
                "MT;1;866425031379169;R2;181129081017+0,21681,20616,460+4+3976+0"));

        verifyAttributes(decoder, text(
                "MT;1;866425031379169;R3;181129081017+0,167910723,14924,460,176+4+3976+0"));

        verifyAttributes(decoder, text(
                "MT;6;866425031377981;R12;190108024848+6a:db:54:5a:79:6d,-91,00:9a:cd:a2:e6:21,-94+0,21681,20616,460+3+3831+0"));

        verifyAttributes(decoder, text(
                "MT;6;866425031377981;R13;190108024848+6a:db:54:5a:79:6d,-91,00:9a:cd:a2:e6:21,-94+0,167910723,14924,460,176+3+3831+0"));

        verifyAttributes(decoder, text(
                "MT;5;866425031379169;RH;5+190116112648+0+0+0+0+11+3954+1"));

    }

}
