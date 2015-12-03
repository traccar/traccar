package org.traccar;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Assert;
import org.traccar.model.Command;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class ProtocolEncoderTest {

    private String concatenateStrings(String... strings) {
        StringBuilder builder = new StringBuilder();
        for (String s : strings) {
            builder.append(s);
        }
        return builder.toString();
    }

    protected ChannelBuffer binary(String... data) {
        return binary(ByteOrder.BIG_ENDIAN, data);
    }

    protected ChannelBuffer binary(ByteOrder endianness, String... data) {
        return ChannelBuffers.wrappedBuffer(
                endianness, DatatypeConverter.parseHexBinary(concatenateStrings(data)));
    }

    protected String text(String... data) {
        return concatenateStrings(data);
    }

    protected ChannelBuffer buffer(String... data) {
        return ChannelBuffers.copiedBuffer(concatenateStrings(data), Charset.defaultCharset());
    }

    protected void verifyCommand(
            BaseProtocolEncoder encoder, Command command, ChannelBuffer expected) throws Exception {
        verifyDecodedCommand(encoder.encodeCommand(command), expected);
    }

    private void verifyDecodedCommand(Object decodedObject, ChannelBuffer expected) {

        Assert.assertNotNull("command is null", decodedObject);
        Assert.assertTrue("not a buffer", decodedObject instanceof ChannelBuffer);
        Assert.assertEquals(ChannelBuffers.hexDump(expected), ChannelBuffers.hexDump((ChannelBuffer) decodedObject));

    }

}
