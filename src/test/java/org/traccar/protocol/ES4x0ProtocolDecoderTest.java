/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class ES4x0ProtocolDecoderTest extends ProtocolTest {

    // Test data format:
    // Header: ET410 (5 bytes: 45 54 34 31 30)
    // IMEI: 8 bytes BCD (0086011102011438)
    // Type: 1 byte (52=Regular, 4D=Maintenance, 4F=OBD)
    // Seq: 1 byte
    // Time: 4 bytes (Unix timestamp)
    // Mask: 2 bytes (bit flags for data fields)
    // Data: variable length based on mask

    // Regular message: type=0x52, mask=0x0018 (bits 3,4 = lat+lon)
    @Test
    public void testDecodeRegular() throws Exception {
        var decoder = inject(new ES4x0ProtocolDecoder(null));
        Position position = new Position(decoder.getProtocolName());
        position.setTime(new java.util.Date(949965330000L));
        position.setLatitude(31.2304);
        position.setLongitude(121.4737);
        position.setValid(true);
        verifyPosition(decoder, binary(
                "455434313000860111020114385214389F52120018129D618048676A68"), position);
    }

    // Regular message with speed: type=0x52, mask=0x0038 (bits 3,4,5 = lat+lon+speed)
    @Test
    public void testDecodeRegularWithSpeed() throws Exception {
        var decoder = inject(new ES4x0ProtocolDecoder(null));
        Position position = new Position(decoder.getProtocolName());
        position.setTime(new java.util.Date(949965330000L));
        position.setLatitude(31.2304);
        position.setLongitude(121.4737);
        position.setSpeed(100.0);
        position.setValid(true);
        verifyPosition(decoder, binary(
                "455434313000860111020114385214389F52120038129D618048676A6800002710"), position);
    }

    // Regular message with course: type=0x52, mask=0x0058 (bits 3,4,6 = lat+lon+course)
    @Test
    public void testDecodeRegularWithCourse() throws Exception {
        var decoder = inject(new ES4x0ProtocolDecoder(null));
        Position position = new Position(decoder.getProtocolName());
        position.setTime(new java.util.Date(949965330000L));
        position.setLatitude(31.2304);
        position.setLongitude(121.4737);
        position.setCourse(90);
        position.setValid(true);
        verifyPosition(decoder, binary(
                "455434313000860111020114385214389F52120058129D618048676A68005A"), position);
    }

    // Regular message with satellites: type=0x52, mask=0x0001 (bit 0 = satellites)
    @Test
    public void testDecodeRegularWithSatellites() throws Exception {
        var decoder = inject(new ES4x0ProtocolDecoder(null));
        verifyAttribute(decoder, binary(
                "455434313000860111020114385214389F521200010801"),
                Position.KEY_SATELLITES, 8);
    }

    // Regular message with HDOP: type=0x52, mask=0x0002 (bit 1 = HDOP)
    @Test
    public void testDecodeRegularWithHdop() throws Exception {
        var decoder = inject(new ES4x0ProtocolDecoder(null));
        verifyAttribute(decoder, binary(
                "455434313000860111020114385214389F521200020501"),
                Position.KEY_HDOP, 0.5);
    }

    // Maintenance message: type=0x4D, mask=0x0001 (bit 0 = event)
    @Test
    public void testDecodeMaintenance() throws Exception {
        var decoder = inject(new ES4x0ProtocolDecoder(null));
        verifyAttribute(decoder, binary(
                "455434313000860111020114384D14389F5212000101"),
                Position.KEY_EVENT, 1);
    }

    // OBD message: type=0x4F, mask=0x0001 (bit 0 = event)
    @Test
    public void testDecodeObd() throws Exception {
        var decoder = inject(new ES4x0ProtocolDecoder(null));
        verifyAttribute(decoder, binary(
                "455434313000860111020114384F14389F5212000101"),
                Position.KEY_EVENT, 1);
    }

}
