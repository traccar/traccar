package org.traccar.protocol;

import org.traccar.BaseFrameDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class ItrProtocolFrameDecoder extends BaseFrameDecoder {

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        if (buf.readableBytes() < 5) {
            return null;
        }

        buf.markReaderIndex();
        int startMarker = buf.readUnsignedShort();
        
        if (startMarker != 0x2828) {  // Verifica o marcador do protocolo
            buf.resetReaderIndex();
            return null;
        }

        int length = buf.getUnsignedShort(buf.readerIndex() + 2);
        
        if (buf.readableBytes() >= length + 5) {
            return buf.readRetainedSlice(length + 5);
        }

        buf.resetReaderIndex();
        return null;
    }
}
