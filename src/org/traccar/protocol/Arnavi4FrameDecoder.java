package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 * Created by Ivan Muratov @binakot on 12.07.2017.
 */
public class Arnavi4FrameDecoder extends FrameDecoder {

    static final byte HEADER_START_SIGN = (byte) 0xFF;
    static final byte HEADER_VERSION_1 = 0x22;
    static final byte HEADER_VERSION_2 = 0x23;
    static final int HEADER_LENGTH = 10;

    static final byte PACKAGE_START_SIGN = 0x5B;
    static final byte PACKAGE_END_SIGN = 0x5D;
    static final int PACKAGE_MIN_PARCEL_NUMBER = 0x01;
    static final int PACKAGE_MAX_PARCEL_NUMBER = 0xFB;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx,
            Channel channel,
            ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() == 0) {
            return null;
        }

        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(0, bytes);

        if (bytes[0] == HEADER_START_SIGN
                && bytes.length == HEADER_LENGTH
                && (bytes[1] == HEADER_VERSION_1 || bytes[1] == HEADER_VERSION_2)) {
            return buf.readBytes(HEADER_LENGTH);
        }

        int parcelNumber = bytes[1] & 0xFF;
        if (bytes[0] == PACKAGE_START_SIGN && bytes[bytes.length - 1] == PACKAGE_END_SIGN
                && parcelNumber >= PACKAGE_MIN_PARCEL_NUMBER && parcelNumber <= PACKAGE_MAX_PARCEL_NUMBER) {
            return buf.readBytes(bytes.length);
        }

        return null;
    }

}
