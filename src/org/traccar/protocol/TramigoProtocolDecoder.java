/*
 * Copyright 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.BaseProtocolDecoder;
import org.traccar.database.DataManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

import java.util.Date;
import java.util.Properties;

public class TramigoProtocolDecoder extends BaseProtocolDecoder {

    public TramigoProtocolDecoder(DataManager dataManager, String protocol, Properties properties) {
        super(dataManager, protocol, properties);
    }

    private static final int MSG_COMPACT = 0x0100;
    private static final int MSG_FULL = 0x00FE;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        if (buf.readUnsignedByte() != 1) {
            return null; // wrong protocol version
        }

        buf.readUnsignedByte(); // version id
        int index = buf.readUnsignedShort();
        int type = buf.readUnsignedShort();
        buf.readUnsignedShort(); // length
        buf.readUnsignedShort(); // mask
        buf.readUnsignedShort(); // checksum
        long id = buf.readUnsignedInt();
        buf.readUnsignedInt(); // time

        if (type == MSG_COMPACT || type == MSG_FULL) {

            // Create new position
            Position position = new Position();
            ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter(getProtocol());
            extendedInfo.set("index", index);

            // Get device id
            try {
                position.setDeviceId(getDataManager().getDeviceByImei(String.valueOf(id)).getId());
            } catch(Exception error) {
                Log.warning("Unknown device - " + id);
                return null;
            }

            buf.readUnsignedShort(); // report trigger
            buf.readUnsignedShort(); // state flag

            position.setValid(true);
            position.setLatitude(buf.readUnsignedInt() * 0.0000001);
            position.setLongitude(buf.readUnsignedInt() * 0.0000001);
            position.setAltitude(0.0);

            buf.readUnsignedShort(); // GSM signal quality
            buf.readUnsignedShort(); // satellites in fix
            buf.readUnsignedShort(); // satellites in track
            buf.readUnsignedShort(); // GPS antenna state

            position.setSpeed(buf.readUnsignedShort() * 0.194384);
            position.setCourse((double )buf.readUnsignedShort());

            buf.readUnsignedInt(); // distance

            extendedInfo.set("battery", buf.readUnsignedShort());

            buf.readUnsignedShort(); // battery charger status

            position.setTime(new Date(buf.readUnsignedInt() * 1000));

            // TODO: parse other data

            position.setExtendedInfo(extendedInfo.toString());
            return position;
        }

        return null;
    }

}
