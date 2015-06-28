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

import java.util.ArrayList;
import java.util.Date;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class AplicomProtocolDecoder extends BaseProtocolDecoder {

    public AplicomProtocolDecoder(AplicomProtocol protocol) {
        super(protocol);
    }

    private static final long IMEI_BASE_TC65_V20 = 0x1437207000000L;
    private static final long IMEI_BASE_TC65_V28 = 358244010000000L;
    private static final long IMEI_BASE_TC65I_V11 = 0x14143B4000000L;

    private static long imeiFromUnitId(long unitId) {
        if(unitId == 0) {
            return 0L;
        } else if(unitId < 0x1000000) {
            // Assume TC65i.
            long imei = IMEI_BASE_TC65I_V11 + unitId;
            if(validateImei(imei)) {
                return imei;
            }
            
            // No? Maybe it's TC65 v2.8?
            imei = IMEI_BASE_TC65_V28 + ((unitId + 0xA8180) & 0xFFFFFF);
            if(validateImei(imei)) {
                return imei;
            }
            
            // Still no match? How about TC65 v2.0?
            imei = IMEI_BASE_TC65_V20 + unitId;
            if(validateImei(imei)) {
                return imei;
            }
        } else {
            // Unit ID is full IMEI, just check it.
            if(validateImei(unitId)) {
                return unitId;
            }
        }
        
        return unitId;
    }
    
    private static boolean validateImei(long imei2) {

        int checksum = 0;
        long remain = imei2;

        // Iterate through all meaningful digits.
        for (int i = 0; remain != 0; i++) {
            // Extract the rightmost digit, that is, compute
            // imei modulo 10 (or remainder of imei / 10).
            int digit = (int) (remain % 10);

            // For each even-positioned digit, calculate the value
            // to be added to sum: Double the digit and then sum up
            // the digits of the result.
            // Example: 7 -> 2*7 = 14 -> 1 + 4 = 5
            if (0 != (i % 2)) {
                digit = digit * 2;
                if (digit >= 10) {
                    digit = digit - 9;
                }
            }
            checksum = checksum + digit;

            // Remove the rightmost digit as it's already processed.
            remain = remain / 10;
        }

        // The IMEI is valid if the calculated checksum is dividable by 10.
        return 0 == (checksum % 10);
    }
    
    private static final int DEFAULT_SELECTOR = 0x0002FC;

    private static final int EVENT_DATA = 119;

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        buf.readUnsignedByte(); // marker
        int version = buf.readUnsignedByte();
        if ((version & 0x80) != 0) {
            buf.skipBytes(4); // unit id high
        }

        String imei = String.valueOf(imeiFromUnitId(buf.readUnsignedMedium()));

        buf.readUnsignedShort(); // length

        // Selector
        int selector = DEFAULT_SELECTOR; // default selector
        if ((version & 0x40) != 0) {
            selector = buf.readUnsignedMedium();
        }

        // Create new position
        Position position = new Position();
        position.setProtocol(getProtocolName());
        if (!identify(imei, channel)) {
            return null;
        }

        position.setDeviceId(getDeviceId());

        // Event
        int event = buf.readUnsignedByte();
        position.set(Event.KEY_EVENT, event);
        buf.readUnsignedByte();

        // Validity
        if ((selector & 0x0008) != 0) {
            position.setValid((buf.readUnsignedByte() & 0x40) != 0);
        } else {
            return null; // no location data
        }

        // Time
        if ((selector & 0x0004) != 0) {
            buf.skipBytes(4); // snapshot time
        }

        // Location
        if ((selector & 0x0008) != 0) {
            position.setTime(new Date(buf.readUnsignedInt() * 1000));
            position.setLatitude(buf.readInt() / 1000000.0);
            position.setLongitude(buf.readInt() / 1000000.0);
            position.set(Event.KEY_SATELLITES, buf.readUnsignedByte());
        }

        // Speed and heading
        if ((selector & 0x0010) != 0) {
            position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
            buf.readUnsignedByte(); // maximum speed
            position.setCourse(buf.readUnsignedByte() * 2.0);
        }

        // Input
        if ((selector & 0x0040) != 0) {
            position.set(Event.KEY_INPUT, buf.readUnsignedByte());
        }
        
        // ADC
        if ((selector & 0x0020) != 0) {
            position.set(Event.PREFIX_ADC + 1, buf.readUnsignedShort());
            position.set(Event.PREFIX_ADC + 2, buf.readUnsignedShort());
            position.set(Event.PREFIX_ADC + 3, buf.readUnsignedShort());
            position.set(Event.PREFIX_ADC + 4, buf.readUnsignedShort());
        }

        // Power
        if ((selector & 0x8000) != 0) {
            position.set(Event.KEY_POWER, buf.readUnsignedShort() / 1000.0);
            position.set(Event.KEY_BATTERY, buf.readUnsignedShort());
        }
        
        // Pulse rate 1
        if ((selector & 0x10000) != 0) {
            buf.readUnsignedShort();
            buf.readUnsignedInt();
        }
        
        // Pulse rate 2
        if ((selector & 0x20000) != 0) {
            buf.readUnsignedShort();
            buf.readUnsignedInt();
        }

        // Trip 1
        if ((selector & 0x0080) != 0) {
            position.set("trip1", buf.readUnsignedInt());
        }

        // Trip 2
        if ((selector & 0x0100) != 0) {
            position.set("trip2", buf.readUnsignedInt());
        }

        // Output
        if ((selector & 0x0040) != 0) {
            position.set(Event.KEY_OUTPUT, buf.readUnsignedByte());
        }
        
        // Button
        if ((selector & 0x0200) != 0) {
            buf.skipBytes(6);
        }
        
        // Keypad
        if ((selector & 0x0400) != 0) {
            buf.readUnsignedByte();
        }
        
        // Altitude
        if ((selector & 0x0800) != 0) {
            position.setAltitude(buf.readShort());
        }

        // Snapshot counter
        if ((selector & 0x2000) != 0) {
            buf.readUnsignedShort();
        }

        // State flags
        if ((selector & 0x4000) != 0) {
            buf.skipBytes(8);
        }

        // Event specific data
        if ((selector & 0x1000) != 0) {
            switch (event) {
                case 2:
                case 40:
                    buf.readUnsignedByte();
                    break;
                case 9:
                    buf.readUnsignedMedium();
                    break;
                case 31:
                case 32:
                    buf.readUnsignedShort();
                    break;
                case 38:
                    buf.skipBytes(4 * 9);
                    break;
                case 113:
                    buf.readUnsignedInt();
                    buf.readUnsignedByte();
                    break;
                case 121:
                case 142:
                    buf.readLong();
                    break;
                case 130:
                    buf.readUnsignedInt(); // incorrect
                    break;
            }
        }

        return position;
    }

}
