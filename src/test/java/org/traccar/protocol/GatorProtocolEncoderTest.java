package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GatorProtocolEncoderTest extends ProtocolTest {

    @Test
    void encodeId() throws  Exception {
        var encoder = inject(new GatorProtocolEncoder(null));
        assertEquals("2008958C", encoder.encodeId(13332082112L));

    }
}
