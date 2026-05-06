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
import java.util.Date;
import io.netty.buffer.ByteBuf;

public class Es4x0ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {
        var decoder = inject(new Es4x0ProtocolDecoder(null));

        verifyPosition(decoder, binary(
                "455434313000860111020114385214389F52120018129D618048676A68"));

        Position expectedSpeedPos = new Position(decoder.getProtocolName());
        expectedSpeedPos.setTime(new Date(949965330000L));
        expectedSpeedPos.setLatitude(31.2304);
        expectedSpeedPos.setLongitude(121.4737);
        expectedSpeedPos.setSpeed(360.0);
        expectedSpeedPos.setValid(true);
        verifyPosition(decoder, binary(
                "455434313000860111020114385214389F52120038129D618048676A6800002710"),
                expectedSpeedPos);

        Position expectedCoursePos = new Position(decoder.getProtocolName());
        expectedCoursePos.setTime(new Date(949965330000L));
        expectedCoursePos.setLatitude(31.2304);
        expectedCoursePos.setLongitude(121.4737);
        expectedCoursePos.setCourse(90.0);
        expectedCoursePos.setValid(true);
        verifyPosition(decoder, binary(
                "455434313000860111020114385214389F52120058129D618048676A68005A"),
                expectedCoursePos);

        verifyAttribute(decoder, binary(
                "455434313000860111020114385214389F521200010801"),
                Position.KEY_SATELLITES, 8);

        verifyAttribute(decoder, binary(
                "455434313000860111020114385214389F521200020501"),
                Position.KEY_HDOP, 0.5);

        verifyAttribute(decoder, binary(
                "455434313000860111020114384D14389F5212000101"),
                Position.KEY_EVENT, 1);

        verifyAttribute(decoder, binary(
                "455434313000860111020114384F14389F5212000101"),
                Position.KEY_EVENT, 1);
    }

    @Test
    public void testDecodeWithLogs() throws Exception {
    var decoder = inject(new Es4x0ProtocolDecoder(null));

    // Speed test
    ByteBuf buf1 = binary("455434313000860111020114385214389F52120038129D618048676A6800002710");
    Position pos1 = (Position) decoder.decode(null, null, buf1);
    System.out.println("Decoded position with speed:");
    System.out.println("  time: " + pos1.getFixTime());
    System.out.println("  lat: " + pos1.getLatitude());
    System.out.println("  lon: " + pos1.getLongitude());
    System.out.println("  speed: " + pos1.getSpeed());
    System.out.println("  valid: " + pos1.getValid());

    // Course test
    ByteBuf buf2 = binary("455434313000860111020114385214389F52120058129D618048676A68005A");
    Position pos2 = (Position) decoder.decode(null, null, buf2);
    System.out.println("\nDecoded position with course:");
    System.out.println("  time: " + pos2.getFixTime());
    System.out.println("  lat: " + pos2.getLatitude());
    System.out.println("  lon: " + pos2.getLongitude());
    System.out.println("  course: " + pos2.getCourse());
    System.out.println("  valid: " + pos2.getValid());

    // Maintenance message
    ByteBuf buf3 = binary("455434313000860111020114384D14389F5212000101");
    Position pos3 = (Position) decoder.decode(null, null, buf3);
    System.out.println("\nDecoded maintenance:");
    System.out.println("  event: " + pos3.getInteger(Position.KEY_EVENT));
    System.out.println("  valid: " + pos3.getValid());

    // OBD message
    ByteBuf buf4 = binary("455434313000860111020114384F14389F5212000101");
    Position pos4 = (Position) decoder.decode(null, null, buf4);
    System.out.println("\nDecoded OBD:");
    System.out.println("  event: " + pos4.getInteger(Position.KEY_EVENT));
    System.out.println("  valid: " + pos4.getValid());
}

}
