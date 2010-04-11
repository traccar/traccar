/*
 * Минимум - 100
 * Начало - (10 цифр) + ','
 * Длина 16 x ','
 */

package net.sourceforge.opentracking.protocol.xexun;

import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * 
 */
public class XexunFrameDecoder extends FrameDecoder {

    protected Object decode(
            ChannelHandlerContext ctx,
            Channel channel,
            ChannelBuffer buf) throws Exception {

        //System.out.println("read: " + buf.readableBytes());

        // Check minimum length
        int length = buf.readableBytes();
        if (length < 100) {
            return null;
        }

        // Find identifier
        int index = 0;
        int countDigit = 0;
        for (; index < length; index++) {

            // Check byte
            char c = (char) buf.getByte(index);
            if (Character.isDigit(c)) {
                countDigit++;
            }

            // Check count
            if (countDigit == 10) {
                break;
            }
        }

        if (countDigit < 10) {
            return null;
        }

        // Find begin
        int beginIndex = 0;
        for (; index < length; index++) {

            char c = (char) buf.getByte(index);
            if (c == ',') {
                beginIndex = index;
                break;
            }
        }

        if (beginIndex == 0) {
            return null;
        }

        // Find imei
        int imeiIndex = 0;
        for (; index < length; index++) {

            char c = (char) buf.getByte(index);
            if (c == ':') {
                imeiIndex = index;
                break;
            }
        }

        if (imeiIndex == 0) {
            return null;
        }

        // Find end
        int endIndex = 0;
        for (; index < length; index++) {

            char c = (char) buf.getByte(index);
            if (c == ',') {
                endIndex = index;
                break;
            }
        }

        if (endIndex == 0) {
            return null;
        }

        // Read buffer
        buf.skipBytes(beginIndex);
        ChannelBuffer frame = buf.readBytes(endIndex - beginIndex + 1);

        return frame;
    }

}
