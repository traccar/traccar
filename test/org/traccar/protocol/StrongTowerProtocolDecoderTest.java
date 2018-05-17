package org.traccar.protocol;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.traccar.ProtocolTest;

public class StrongTowerProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        StrongTowerProtocolDecoder decoder = new StrongTowerProtocolDecoder(new StrongTowerProtocol());

        verifyNotNull(decoder, text(
                "{\"id\":\"864895030279986\",\"timestamp\":\"2018-05-13 12:00:00\",\"lat\":3.24,\"lon\":6.5,\"speed\":234,\"valid\":true}")
        );

        verifyAttributes(decoder, text(
                "{\"id\":\"864895030279986\",\"timestamp\":\"2018-05-13 12:00:00\",\"lat\":3.24,\"lon\":6.5,\"speed\":234,\"valid\":true,\"alarm\":\"general\"}")
        );

        verifyPosition(decoder, text(
                "{\"id\":\"864895030279986\",\"timestamp\":\"2018-05-13 12:00:00\",\"lat\":3.24,\"lon\":6.5,\"speed\":234,\"valid\":true}"),
                position("2018-05-13 12:00:00.0", true, 3.24, 6.5)
        );

        verifyPosition(decoder, text(
                "{\"id\":\"864895030279986\",\"timestamp\":\"2018-05-13 12:00:00\",\"lat\":3.24,\"lon\":6.5,\"speed\":234,\"valid\":true}")
        );

        try {
            verifyNull(decoder, text(
                    ""));
            assertFalse(true);
        } catch (Exception e) {
            assertTrue(true);
        }
        try {
            verifyNull(decoder, text(
                    "{}"));
        } catch (Exception e) {
            assertTrue(true);
        }
        try {
            verifyNull(decoder, text(
                    ":"));
            assertFalse(true);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            verifyNull(decoder, text(
                    "{id:864895030279986,timestamp:2018-05-13 12:00:00,lat:3.24,lon:6.5,speed:234}")
            );
            assertFalse(true);
        } catch (Exception e) {
            assertTrue(true);
        }
    }

}
