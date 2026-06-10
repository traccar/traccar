package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class MictrackHQProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new MictrackHQProtocolDecoder(null));

        // V1 heartbeat
        verifyPosition(decoder, text(
                "*HQ,8168000008,V1,043602,A,2234.9273,N,11354.3980,E,000.06,000,100715,FBFFBBFF,460,00,10342,4283"),
                position("2015-07-10 04:36:02.000", true, 22.58212, 113.90664));

        // V5 with mileage and external power voltage
        verifyPosition(decoder, text(
                "*HQ,8168000008,V5,043602,A,2234.9273,N,11354.3980,E,000.06,000,100715,FBFFBBFF,460,00,10342,4283,1000,125"));

        // V6 with ICCID
        verifyPosition(decoder, text(
                "*HQ,8168000008,V6,043602,A,2234.9273,N,11354.3980,E,000.06,000,100715,FBFFBBFF,460,00,10342,4283,898602A2091508006821"));

        // SOS alarm (byte4 bit1 = 0)
        verifyAttribute(decoder, text(
                "*HQ,8168000008,V1,043602,A,2234.9273,N,11354.3980,E,000.06,000,100715,FBFFBBFD,460,00,10342,4283"),
                Position.KEY_ALARM, Position.ALARM_SOS);

        // Overspeed alarm (byte4 bit2 = 0)
        verifyAttribute(decoder, text(
                "*HQ,8168000008,V1,043602,A,2234.9273,N,11354.3980,E,000.06,000,100715,FBFFBBFB,460,00,10342,4283"),
                Position.KEY_ALARM, Position.ALARM_OVERSPEED);

    }

}
