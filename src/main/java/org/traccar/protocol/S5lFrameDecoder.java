package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.traccar.BaseFrameDecoder;

public class S5lFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, ByteBuf buf) throws Exception {

        if (buf.readableBytes() < 5) {
            return null;
        }

        // Start bit is always 0x78 0x78
        int length = 2 + 1 + buf.getUnsignedByte(buf.readerIndex() + 2) + 2; // start + length field + data + stop

        if (buf.readableBytes() >= length && buf.getUnsignedShort(buf.readerIndex() + length - 2) == 0x0d0a) {
            return buf.readRetainedSlice(length);
        }

        int endIndex = buf.readerIndex() - 1;
        do {
            endIndex = buf.indexOf(endIndex + 1, buf.writerIndex(), (byte) 0x0d);
            if (endIndex > 0 && buf.writerIndex() > endIndex + 1 && buf.getByte(endIndex + 1) == 0x0a) {
                return buf.readRetainedSlice(endIndex + 2 - buf.readerIndex());
            }
        } while (endIndex > 0);

        return null;
    }

}