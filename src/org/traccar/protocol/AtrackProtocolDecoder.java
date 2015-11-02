/*
 * Copyright 2013 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class AtrackProtocolDecoder extends BaseProtocolDecoder {

    private static final int MIN_DATA_LENGTH = 40;

    private boolean longDate;
    private boolean custom;
    private String form;

    public AtrackProtocolDecoder(AtrackProtocol protocol) {
        super(protocol);

        longDate = Context.getConfig().getBoolean(getProtocolName() + ".longDate");

        custom = Context.getConfig().getBoolean(getProtocolName() + ".custom");
        form = Context.getConfig().getString(getProtocolName() + ".form");
        if (form != null) {
            custom = true;
        }
    }

    public void setLongDate(boolean longDate) {
        this.longDate = longDate;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    private static void sendResponse(Channel channel, SocketAddress remoteAddress, long rawId, int index) {
        if (channel != null) {
            ChannelBuffer response = ChannelBuffers.directBuffer(12);
            response.writeShort(0xfe02);
            response.writeLong(rawId);
            response.writeShort(index);
            channel.write(response, remoteAddress);
        }
    }

    private static String readString(ChannelBuffer buf) {
        String result = null;
        int index = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) 0);
        if (index > buf.readerIndex()) {
            result = buf.readBytes(index - buf.readerIndex()).toString(Charset.defaultCharset());
        }
        buf.readByte();
        return result;
    }

    private void readCustomData(Position position, ChannelBuffer buf, String form) {
        String[] keys = form.substring(1).split("%");
        for (String key : keys) {
            switch (key) {
                case "SA":
                    position.set(Event.KEY_SATELLITES, buf.readUnsignedByte());
                    break;
                case "MV":
                    position.set(Event.KEY_POWER, buf.readUnsignedShort());
                    break;
                case "BV":
                    position.set(Event.KEY_BATTERY, buf.readUnsignedShort());
                    break;
                case "GQ":
                    position.set(Event.KEY_GSM, buf.readUnsignedByte());
                    break;
                case "CE":
                    position.set(Event.KEY_CELL, buf.readUnsignedInt());
                    break;
                case "LC":
                    position.set(Event.KEY_LAC, buf.readUnsignedShort());
                    break;
                case "CN":
                    buf.readUnsignedInt(); // mcc + mnc
                    break;
                case "RL":
                    buf.readUnsignedByte(); // rxlev
                    break;
                case "PC":
                    buf.readUnsignedInt(); // pulse count
                    break;
                case "AT":
                    position.setAltitude(buf.readUnsignedInt());
                    break;
                case "RP":
                    position.set(Event.KEY_RPM, buf.readUnsignedShort());
                    break;
                case "GS":
                    buf.readUnsignedByte(); // gsm status
                    break;
                case "DT":
                    position.set(Event.KEY_ARCHIVE, buf.readUnsignedByte() == 1);
                    break;
                case "VN":
                    position.set(Event.KEY_VIN, readString(buf));
                    break;
                case "MF":
                    buf.readUnsignedShort(); // mass air flow rate
                    break;
                case "EL":
                    buf.readUnsignedByte(); // engine load
                    break;
                case "TR":
                    buf.readUnsignedByte(); // throttle position
                    break;
                case "ET":
                    buf.readUnsignedShort(); // engine coolant temp
                    break;
                case "FL":
                    position.set(Event.KEY_FUEL, buf.readUnsignedByte());
                    break;
                case "ML":
                    buf.readUnsignedByte(); // mil status
                    break;
                case "FC":
                    buf.readUnsignedInt(); // fuel used
                    break;
                case "CI":
                    readString(buf); // format string
                    break;
                case "AV1":
                    position.set(Event.PREFIX_ADC + 1, buf.readUnsignedShort());
                    break;
                case "NC":
                    readString(buf); // gsm neighbor cell info
                    break;
                case "SM":
                    buf.readUnsignedShort(); // max speed between reports
                    break;
                case "GL":
                    readString(buf); // google link
                    break;
                case "MA":
                    readString(buf); // mac address
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        if (buf.getUnsignedShort(buf.readerIndex()) == 0xfe02) {
            if (channel != null) {
                channel.write(buf, remoteAddress); // keep-alive message
            }
            return null;
        }

        buf.skipBytes(2); // prefix
        buf.readUnsignedShort(); // checksum
        buf.readUnsignedShort(); // length
        int index = buf.readUnsignedShort();

        long id = buf.readLong();
        if (!identify(String.valueOf(id), channel, remoteAddress)) {
            return null;
        }

        sendResponse(channel, remoteAddress, id, index);

        List<Position> positions = new LinkedList<>();

        while (buf.readableBytes() >= MIN_DATA_LENGTH) {

            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(getDeviceId());

            if (longDate) {

                DateBuilder dateBuilder = new DateBuilder()
                        .setDate(buf.readUnsignedShort(), buf.readUnsignedByte(), buf.readUnsignedByte())
                        .setTime(buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte());
                position.setTime(dateBuilder.getDate());

                buf.skipBytes(7 + 7);


            } else {

                position.setFixTime(new Date(buf.readUnsignedInt() * 1000));
                position.setDeviceTime(new Date(buf.readUnsignedInt() * 1000));
                buf.readUnsignedInt(); // send time
            }

            position.setValid(true);
            position.setLongitude(buf.readInt() * 0.000001);
            position.setLatitude(buf.readInt() * 0.000001);
            position.setCourse(buf.readUnsignedShort());

            position.set(Event.KEY_TYPE, buf.readUnsignedByte());
            position.set(Event.KEY_ODOMETER, buf.readUnsignedInt() * 0.1);
            position.set(Event.KEY_HDOP, buf.readUnsignedShort() * 0.1);
            position.set(Event.KEY_INPUT, buf.readUnsignedByte());

            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));

            position.set(Event.KEY_OUTPUT, buf.readUnsignedByte());
            position.set(Event.PREFIX_ADC + 1, buf.readUnsignedShort() * 0.001);

            position.set("driver", readString(buf));

            position.set(Event.PREFIX_TEMP + 1, buf.readShort() * 0.1);
            position.set(Event.PREFIX_TEMP + 2, buf.readShort() * 0.1);

            position.set("message", readString(buf));

            if (custom) {
                String form = this.form;
                if (form == null) {
                    form = readString(buf).substring("%CI".length());
                }
                readCustomData(position, buf, form);
            }

            positions.add(position);

        }

        return positions;
    }

}
