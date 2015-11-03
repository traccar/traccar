package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolDecoderTest;

public class GpsGateProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        GpsGateProtocolDecoder decoder = new GpsGateProtocolDecoder(new GpsGateProtocol());

        verifyNothing(decoder, text(
                "$FRLIN,,user1,8IVHF*7A"));
        
        verifyNothing(decoder, text(
                "$FRLIN,,354503026292842,VGZTHKT*0C"));

        verifyNothing(decoder, text(
                "$FRLIN,IMEI,1234123412341234,*7B"));
        
        verifyNothing(decoder, text(
                "$FRLIN,,saab93_device,KLRFBGIVDJ*28"));

        verifyPosition(decoder, text(
                "$GPRMC,154403.000,A,6311.64120,N,01438.02740,E,0.000,0.0,270707,,*0A"),
                position("2007-07-27 15:44:03.000", true, 63.19402, 14.63379));

        verifyPosition(decoder, text(
                "$GPRMC,074524,A,5553.73701,N,03728.90491,E,10.39,226.5,160614,0.0,E*75"));

        verifyPosition(decoder, text(
                "$GPRMC,154403.000,A,6311.64120,N,01438.02740,E,0.000,0.0,270707,,*0A"));

    }

}
