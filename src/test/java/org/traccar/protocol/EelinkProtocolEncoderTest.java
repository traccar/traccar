package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramChannel;
import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EelinkProtocolEncoderTest extends ProtocolTest {

    @Test
    public void testEncodeTcp() throws Exception {

        var decoder = mock(EelinkProtocolDecoder.class);
        var encoder = inject(new EelinkProtocolEncoder(null));
        var channel = channel(decoder, encoder);

        when(decoder.getHeader()).thenReturn(EelinkProtocolDecoder.HEADER_EELINK);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        verifyEncode(channel, command,
                binary("676780000f0000010000000052454c41592c3123"));

        when(decoder.getHeader()).thenReturn(EelinkProtocolDecoder.HEADER_ITR120);

        verifyEncode(channel, command,
                binary("282880000f0000010000000052454c41592c3123"));

    }

    @Test
    public void testEncodeUdp() throws Exception {

        var encoder = inject(new EelinkProtocolEncoder(null));
        var channel = mock(DatagramChannel.class);
        var pipeline = mock(ChannelPipeline.class);
        var decoder = mock(EelinkProtocolDecoder.class);

        when(channel.pipeline()).thenReturn(pipeline);
        when(pipeline.get(EelinkProtocolDecoder.class)).thenReturn(decoder);
        when(decoder.getHeader()).thenReturn(EelinkProtocolDecoder.HEADER_EELINK);

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);

        ByteBuf encoded = (ByteBuf) encoder.encodeCommand(channel, command);
        try {
            assertEquals("454c001eb41a0123456789012345676780000f0000010000000052454c41592c3123",
                    ByteBufUtil.hexDump(encoded));
        } finally {
            encoded.release();
        }

    }

}
