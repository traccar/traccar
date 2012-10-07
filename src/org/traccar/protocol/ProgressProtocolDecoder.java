/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.nio.charset.Charset;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.GenericProtocolDecoder;
import org.traccar.model.DataManager;
import org.traccar.model.Position;

/**
 * Progress tracker protocol decoder
 */
public class ProgressProtocolDecoder extends GenericProtocolDecoder {

    /**
     * Device ID
     */
    private long deviceId;

    /**
     * Initialize
     */
    public ProgressProtocolDecoder(DataManager dataManager, Integer resetDelay) {
        super(dataManager, resetDelay);
    }

    /*
     * Message types
     */
    static final int MSG_NULL = 0;
    static final int MSG_IDENT = 1;
    static final int MSG_IDENT_FULL = 2;
    static final int MSG_POINT = 10;
    static final int MSG_LOG_SYNC = 100;
    static final int MSG_LOGMSG = 101;
    static final int MSG_TEXT = 102;
    static final int MSG_ALARM = 200;
    static final int MSG_ALARM_RECIEVED = 201;

    /**
     * Decode message
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;
        int type = buf.readUnsignedShort();
        int length = buf.readUnsignedShort();

        // Authentication
        if (type == MSG_IDENT || type == MSG_IDENT_FULL) {
            long id = buf.readUnsignedInt();
            length = buf.readUnsignedShort();
            buf.skipBytes(length);
            length = buf.readUnsignedShort();
            String imei = buf.readBytes(length).toString(Charset.defaultCharset());
            deviceId = getDataManager().getDeviceByImei(imei).getId();
        }

        // Position
        else if (type == MSG_POINT || type == MSG_ALARM) {
            Position position = new Position();
            position.setDeviceId(deviceId);
            // TODO: parse messages here

            if (type == MSG_ALARM) {
                // TODO: send MSG_ALARM_RECIEVED / channel.write(...);
            }

            return position;
        }

        return null;
    }

}
