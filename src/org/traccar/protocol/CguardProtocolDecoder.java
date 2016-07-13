/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Position;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Arrays;

public class CguardProtocolDecoder extends BaseProtocolDecoder {

    public CguardProtocolDecoder(CguardProtocol protocol) {
        super(protocol);
    }

    public static final String NV_DATA = "NV";
    public static final String BC_DATA = "BC";
    public static final String NAN_DATA = "NAN";
    public static final String VERSION_DATA = "VERSION";
    public static final String IDRO_DATA = "IDRO";
    public static final String ID_DATA = "ID";
    public static final String LOG_DATA = "LOG";
    public static final String BAT = "BAT1";


    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        String bufString = buf.toString(Charset.defaultCharset());
        String[] bufStringRows = bufString.split("\n");
        String dataString = bufStringRows[0];

        if(dataString.startsWith(VERSION_DATA)) {
            String idString = bufStringRows[1];
            if(idString.startsWith(IDRO_DATA)) {
                int lenght = IDRO_DATA.length() + 1;
                String imei = idString.substring(lenght, idString.length());
                identify(imei, channel, remoteAddress);
                long deviceId = getDeviceId();
                Device device = Context.getIdentityManager().getDeviceById(deviceId);
            } else if(idString.startsWith(ID_DATA)) {
                int lenght = ID_DATA.length() + 1;
                String imei = idString.substring(lenght, idString.length());
                identify(imei, channel, remoteAddress);
            }
        } else if(dataString.startsWith(NV_DATA)) {
            Position position = decodeNvData(dataString);
            int arrLength = bufStringRows.length;
            for(int i = 1; i < arrLength; i++) {
                if(bufStringRows[i].startsWith(BC_DATA)) {
                    position = decodeBcData(bufStringRows[i], position);
                }
            }
            return position;
        }

        return null;
    }

    private Position decodeBcData(String dataString, Position position) {
        if(dataString.toLowerCase().contains(BAT.toLowerCase())) {
            String[] bcData = dataString.split(":");
            int batIndex = Arrays.asList(bcData).indexOf(BAT);
            if(!bcData[batIndex + 1].equalsIgnoreCase(NAN_DATA)) {
                double bat = Double.parseDouble(bcData[batIndex + 1]);
                position.set(Position.KEY_BATTERY, bat);
            }
            return position;
        } else {
            return null;
        }
    }

    private Position decodeNvData(String dataString) {
        Position position = new Position();

        position.setProtocol(getProtocolName());
        position.setDeviceId(getDeviceId());

        //read all as String....
        String[] nvData = dataString.split(":");
        int arrLength = nvData.length;

        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyMMdd HHmmss");
        DateTime dt = formatter.parseDateTime(nvData[1]);
        position.setTime(dt.toDate());

        double latitude = Double.parseDouble(nvData[2]);
        position.setLatitude(latitude);

        double longitude = Double.parseDouble(nvData[3]);
        position.setLongitude(longitude);

        double speed =  Double.parseDouble(nvData[4]);
        position.setSpeed(speed);

        if(arrLength >= 7) {
            if(!nvData[6].equalsIgnoreCase(NAN_DATA)) {
                double cource = Double.parseDouble(nvData[6]);
                position.setCourse(cource);
            }
        }

        if(arrLength >= 8) {
            if(!nvData[7].equalsIgnoreCase(NAN_DATA)) {
                double altitude = Double.parseDouble(nvData[7]);
                position.setAltitude(altitude);
            }
        }

        return position;
    }
}
