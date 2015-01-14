package org.traccar;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;

public class CharacterDelimiterFrameDecoder extends DelimiterBasedFrameDecoder {

    private static ChannelBuffer createDelimiter(char delimiter) {
        byte buf[] = { (byte) delimiter };
        return ChannelBuffers.wrappedBuffer(buf);
    }

    private static ChannelBuffer createDelimiter(String delimiter) {
        byte buf[] = new byte[delimiter.length()];
        for (int i = 0; i < delimiter.length(); i++) {
            buf[i] = (byte) delimiter.charAt(i);
        }
        return ChannelBuffers.wrappedBuffer(buf);
    }

    private static ChannelBuffer[] convertDelimiters(String[] delimiters) {
        ChannelBuffer result[] = new ChannelBuffer[delimiters.length];
        for (int i = 0; i < delimiters.length; i++) {
            result[i] = createDelimiter(delimiters[i]);
        }
        return result;
    }

    public CharacterDelimiterFrameDecoder(int maxFrameLength, char delimiter) {
        super(maxFrameLength, createDelimiter(delimiter));
    }

    public CharacterDelimiterFrameDecoder(int maxFrameLength, String delimiter) {
        super(maxFrameLength, createDelimiter(delimiter));
    }

    public CharacterDelimiterFrameDecoder(int maxFrameLength, String... delimiters) {
        super(maxFrameLength, convertDelimiters(delimiters));
    }

}
