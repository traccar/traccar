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

import static org.junit.jupiter.api.Assertions.assertNull;

public class ES4x0FrameDecoderTest extends ProtocolTest {

    // Test data: ET410 (header) + IMEI (0086011102011438) + type (52) + seq (14) + time (389F5212) + mask (0000)
    // Total: 5 + 8 + 1 + 1 + 4 + 2 = 21 bytes, mask=0 means no additional data fields
    @Test
    public void testDecodeFullFrame() throws Exception {
        var decoder = inject(new ES4x0FrameDecoder());
        verifyFrame(
                binary("455434313000860111020114385214389F52120000"),
                decoder.decode(null, null, binary("455434313000860111020114385214389F52120000")));
    }

    @Test
    public void testDecodeIncomplete() throws Exception {
        var decoder = inject(new ES4x0FrameDecoder());
        assertNull(decoder.decode(null, null, binary("455434313000860111020114385214389F52")));
    }

}
