/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
