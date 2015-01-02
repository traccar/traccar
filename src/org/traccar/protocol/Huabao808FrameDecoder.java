package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

public class Huabao808FrameDecoder extends FrameDecoder {

    @Override
    protected Object decode(ChannelHandlerContext channelHandlerContext, Channel channel, ChannelBuffer channelBuffer) throws Exception {

        // Check minimum length and content
        if (channelBuffer.readableBytes() < 31 || channelBuffer.readByte() == -1) {
            return null;
        }

        return channelBuffer.readBytes(channelBuffer.array().length);
    }

}
