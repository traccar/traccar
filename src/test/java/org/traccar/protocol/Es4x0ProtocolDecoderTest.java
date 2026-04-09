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

public class Es4x0ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {
        var decoder = inject(new Es4x0ProtocolDecoder(null));

        verifyPosition(decoder, binary(
                "455434313000860111020114385214389F52120018129D618048676A68"));

        verifyPosition(decoder, binary(
                "455434313000860111020114385214389F52120038129D618048676A6800002710"));

        verifyAttribute(decoder, binary(
                "455434313000860111020114385214389F52120038129D618048676A6800002710"),
                "speed", 194.38444900000002);

        verifyPosition(decoder, binary(
                "455434313000860111020114385214389F52120058129D618048676A68005A"));

        verifyAttribute(decoder, binary(
                "455434313000860111020114385214389F52120058129D618048676A68005A"),
                "course", 90.0);

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

}
