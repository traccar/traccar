package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class Gt02FrameDecoderTest extends ProtocolTest {

    private static final String HEARTBEAT = "68680f0504035889905831401700df1a00000d0a";
    private static final String DATA =
            "68682500a403588990510127660001100e09060a1d1b00ade1c90b79ea3000011b000000000000050d0a";
    private static final String GT06_SHORT = "78780d0103563140414198583c0d0a";
    private static final String GT06_LONG = "79790008940a01020001abcd0d0a";

    @Test
    public void testDecode() throws Exception {

        var channel = channel(inject(new Gt02FrameDecoder()));

        verifyDecode(channel, binary(HEARTBEAT),
                binary(HEARTBEAT));

        verifyDecode(channel, binary(DATA + HEARTBEAT),
                binary(DATA), binary(HEARTBEAT));

    }

    /**
     * Positions keep being decoded after a device sends a GT06 frame on the same connection.
     */
    @Test
    public void testSkipGt06Frames() throws Exception {

        var channel = channel(inject(new Gt02FrameDecoder()));

        // a GT06 frame with a two-byte length must not shift the GT02 frames after it
        verifyDecode(channel, binary(GT06_LONG + DATA),
                binary(DATA));

        verifyDecode(channel, binary(GT06_SHORT + HEARTBEAT),
                binary(HEARTBEAT));

        verifyDecode(channel, binary(DATA + GT06_LONG + GT06_SHORT + HEARTBEAT),
                binary(DATA), binary(HEARTBEAT));

    }

    /**
     * Bytes that are not a frame do not cut the device off: decoding resumes at the
     * next frame on the same connection.
     */
    @Test
    public void testResynchronize() throws Exception {

        var channel = channel(inject(new Gt02FrameDecoder()));

        verifyDecode(channel, binary("0102030d0a" + HEARTBEAT),
                binary(HEARTBEAT));

        // a header with a wrong tail is not a frame
        verifyDecode(channel, binary("68680f0504035889905831401700df1a00000d0b" + DATA),
                binary(DATA));

    }

    @Test
    public void testPartialFrame() throws Exception {

        var channel = channel(inject(new Gt02FrameDecoder()));

        verifyDecode(channel, binary(DATA.substring(0, 30)));

        verifyDecode(channel, binary(DATA.substring(30)),
                binary(DATA));

    }

}
