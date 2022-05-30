/*
 * Copyright 2017 - 2018 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.text.SimpleDateFormat;

public class GenxProtocolDecoder extends BaseProtocolDecoder {

    private int[] reportColumns;

    public GenxProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected void init() {
        setReportColumns(getConfig().getString(getProtocolName() + ".reportColumns", "1,2,3,4"));
    }

    public void setReportColumns(String format) {
        String[] columns = format.split(",");
        reportColumns = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            reportColumns[i] = Integer.parseInt(columns[i]);
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String[] values = ((String) msg).split(",");

        Position position = new Position(getProtocolName());
        position.setValid(true);

        for (int i = 0; i < Math.min(values.length, reportColumns.length); i++) {
            switch (reportColumns[i]) {
                case 1:
                case 28:
                    DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, values[i]);
                    if (deviceSession != null) {
                        position.setDeviceId(deviceSession.getDeviceId());
                    }
                    break;
                case 2:
                    position.setTime(new SimpleDateFormat("MM/dd/yy HH:mm:ss").parse(values[i]));
                    break;
                case 3:
                    position.setLatitude(Double.parseDouble(values[i]));
                    break;
                case 4:
                    position.setLongitude(Double.parseDouble(values[i]));
                    break;
                case 11:
                    position.set(Position.KEY_IGNITION, values[i].equals("ON"));
                    break;
                case 13:
                    position.setSpeed(UnitsConverter.knotsFromKph(Integer.parseInt(values[i])));
                    break;
                case 17:
                    position.setCourse(Integer.parseInt(values[i]));
                    break;
                case 23:
                    position.set(Position.KEY_ODOMETER, Double.parseDouble(values[i]) * 1000);
                    break;
                case 27:
                    position.setAltitude(UnitsConverter.metersFromFeet(Integer.parseInt(values[i])));
                    break;
                case 46:
                    position.set(Position.KEY_SATELLITES, Integer.parseInt(values[i]));
                    break;
                default:
                    break;
            }
        }

        return position.getDeviceId() != 0 ? position : null;
    }

}
