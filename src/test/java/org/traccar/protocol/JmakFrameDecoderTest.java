package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class JmakFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new JmakFrameDecoder());

        // 1) Simple JSON object {"foo":1}
        verifyFrame(
                binary("7B22666F6F223A317D"),
                decoder.decode(null, null, binary("7B22666F6F223A317D"))
        );

        // 2) Nested JSON object {"b":{"c":2}}
        verifyFrame(
                binary("7B2262223A7B2263223A327D7D"),
                decoder.decode(null, null, binary("7B2262223A7B2263223A327D7D"))
        );

        // 3) Simple text frame with '~' prefix and '$' suffix
        verifyFrame(
                binary("7E4142433B3132333B58595A24"),
                decoder.decode(null, null, binary("7E4142433B3132333B58595A24"))
        );

        // 4) Simple ZIP/STRING frame with '^' prefix and '$' suffix
        verifyFrame(
                binary("5E44415441544124"),
                decoder.decode(null, null, binary("5E44415441544124"))
        );

        // 5) Realistic JMAK log message
        verifyFrame(
                binary("7E303030303030303431464533464646463B3233333333332D33333333332D333333333333333B3836383639353036303731353031363B383231303B4E554C4C3B313735303638393731383533303B2D31392E38383234363B2D34332E39373835353B3832352E36303B423B302E35333B31363B33313B302E38373B302E30303B313B303B35322E34353B342E37343B31313735303B333836333B54494D423B343B313735303638393731383534333B313B393B303B302E303024"),
                decoder.decode(null, null, binary("7E303030303030303431464533464646463B3233333333332D33333333332D333333333333333B3836383639353036303731353031363B383231303B4E554C4C3B313735303638393731383533303B2D31392E38383234363B2D34332E39373835353B3832352E36303B423B302E35333B31363B33313B302E38373B302E30303B313B303B35322E34353B342E37343B31313735303B333836333B54494D423B343B313735303638393731383534333B313B393B303B302E303024"))
        );

        // 6) Frame with CRLF after the suffix
        verifyFrame(
                binary("7E4124"),
                decoder.decode(null, null, binary("7E41240D0A"))
        );
    }

}
