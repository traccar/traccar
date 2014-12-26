/*
 * Copyright 2014 Jon S. Stumpf (http://github.com/jon-stumpf)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.traccar.helper.Log;

public class Esky610FrameDecoder extends FrameDecoder {
    
    // 10 is an arbitrary limit
    private final int MESSAGE_MINIMUM_LENGTH = 10;
    private final char BEGIN_FRAME = 'E';
    
    @Override
    protected Object decode(
            ChannelHandlerContext ctx,
            Channel channel,
            ChannelBuffer buf) throws Exception {

        if (buf.readableBytes() < MESSAGE_MINIMUM_LENGTH) {
            return null;
        }

        // Esky frames begin with an 'E'.
        // Sadly, they have no terminator and are variable length.
        // I am working with the manufacturer to improve the protocol.
        Integer index = -1;
        Integer i;
        
        // Get to the beginning of a frame
        for(i = buf.readerIndex(); i < buf.readableBytes(); ++i) {
            if ( (char) buf.getByte(i) == BEGIN_FRAME ) {
                index = i;
                break;
            }
        }

        if ( index < 0 ) {
            return null;
        }

        if ( index > buf.readerIndex() ) {
            // We should be but are not at the beginning of a frame.
            Log.debug("esky610: skipping " + (index - buf.readerIndex()) + " bytes\n");
            
            buf.skipBytes(index - buf.readerIndex());
        }

        // We are at the beginning of the frame

        // Esky does not terminate their variable-length records.  So, I search
        // for the beginning of the next record which delays the transmission
        // of this record.  This is an issue to be fixed later.
        index = -1;
        
        // Get to the beginning of the next frame.
        for(i = buf.readerIndex() + 1; i < buf.readableBytes(); ++i) {
            if ( (char) buf.getByte(i) == BEGIN_FRAME ) {
                index = i;
                break;
            }
        }

        if ( index < 0 ) {
            // The next frame hasn't shown up yet
            Log.debug("esky610: didn't find the next record\n");
            
            // TODO: If the channel has disconnected, we should try to consume
            // what is left instead of discarding it.  Testing for
            // ( ! channel.isConnected() ) does not work.
            
            return null;
        }

        return buf.readBytes(index - buf.readerIndex());
    }
}
