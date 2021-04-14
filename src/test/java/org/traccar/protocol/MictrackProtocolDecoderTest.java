package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class MictrackProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecodeStandard() throws Exception {

        var decoder = new MictrackProtocolDecoder(null);

        verifyAttribute(decoder, text(
                "867035041390699 netlock=Success!"),
                Position.KEY_RESULT, "netlock=Success");

        verifyAttribute(decoder, text(
                "mode=Success!"),
                Position.KEY_RESULT, "mode=Success");

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

    @Test
    public void testDecodeLowAltitude() throws Exception {

        var decoder = new MictrackProtocolDecoder(null);

        verifyPositions(decoder, text(
                "861108032038761$062232.00,A,2238.2832,N,11401.7381,E,0.01,309.62,95.0,131117"));

        verifyPositions(decoder, text(
                "861108032038761$062232.00,A,2238.2832,N,11401.7381,E,0.01,309.62,95.0,131117$062332.00,A,2238.2836,N,11401.7386,E,0.06,209.62,95.0,131117"));

        verifyPositions(decoder, text(
                "861108032038761$062232.00,A,2238.2832,N,11401.7381,E,0.01,309.62,95.0,131117"),
                position("2017-11-13 06:22:32.000", true, 22.63806, 114.028976));
    }

}
