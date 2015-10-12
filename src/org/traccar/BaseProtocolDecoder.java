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
package org.traccar;

import java.net.SocketAddress;
import java.util.Date;

import org.jboss.netty.channel.Channel;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Position;

public abstract class BaseProtocolDecoder extends ExtendedObjectDecoder {

    private final Protocol protocol;

    public String getProtocolName() {
        return protocol.getName();
    }

    private long deviceId;

    public boolean hasDeviceId() {
        return deviceId != 0;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public boolean identify(String uniqueId, Channel channel, SocketAddress remoteAddress, boolean logWarning) {
        try {
            Device device = Context.getIdentityManager().getDeviceByUniqueId(uniqueId);
            if (device != null) {
                deviceId = device.getId();
                Context.getConnectionManager().setActiveDevice(deviceId, protocol, channel, remoteAddress);
                return true;
            } else {
                deviceId = 0;
                if (logWarning) {
                    Log.warning("Unknown device - " + uniqueId);
                }
                return false;
            }
        } catch (Exception error) {
            deviceId = 0;
            Log.warning(error);
            return false;
        }
    }

    public boolean identify(String uniqueId, Channel channel, SocketAddress remoteAddress) {
        return identify(uniqueId, channel, remoteAddress, true);
    }

    public boolean identify(String uniqueId, Channel channel) {
        return identify(uniqueId, channel, null, true);
    }

    public BaseProtocolDecoder(Protocol protocol) {
        this.protocol = protocol;
    }

    public void initPositionDetails(Position position) {
        Position last = Context.getConnectionManager().getLastPosition(getDeviceId());
        if (last != null) {
            position.setFixTime(last.getFixTime());
            position.setValid(last.getValid());
            position.setLatitude(last.getLatitude());
            position.setLongitude(last.getLongitude());
            position.setAltitude(last.getAltitude());
            position.setSpeed(last.getSpeed());
            position.setCourse(last.getCourse());
        } else {
            position.setFixTime(new Date(0));
        }
    }

}
