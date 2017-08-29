/*
 * Copyright 2012 - 2016 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.traccar.helper.Log;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Device;
import org.traccar.model.Position;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.sql.SQLException;

public abstract class BaseProtocolDecoder extends ExtendedObjectDecoder {

    private final Protocol protocol;

    public long addUnknownDevice(String uniqueId) {
        Device device = new Device();
        device.setName(uniqueId);
        device.setUniqueId(uniqueId);
        device.setCategory(Context.getConfig().getString("database.registerUnknown.defaultCategory"));

        long defaultGroupId = Context.getConfig().getLong("database.registerUnknown.defaultGroupId");
        if (defaultGroupId != 0) {
            device.setGroupId(defaultGroupId);
        }

        try {
            Context.getDeviceManager().addItem(device);

            Log.info("Automatically registered device " + uniqueId);

            if (defaultGroupId != 0) {
                Context.getPermissionsManager().refreshDeviceAndGroupPermissions();
                Context.getPermissionsManager().refreshAllExtendedPermissions();
            }

            return device.getId();
        } catch (SQLException e) {
            Log.warning(e);
            return 0;
        }
    }

    public String getProtocolName() {
        return protocol.getName();
    }

    protected double convertSpeed(double value, String defaultUnits) {
        switch (Context.getConfig().getString(getProtocolName() + ".speed", defaultUnits)) {
            case "kmh":
                return UnitsConverter.knotsFromKph(value);
            case "mps":
                return UnitsConverter.knotsFromMps(value);
            case "mph":
                return UnitsConverter.knotsFromMph(value);
            case "kn":
            default:
                return value;
        }
    }

    private DeviceSession channelDeviceSession; // connection-based protocols
    private Map<SocketAddress, DeviceSession> addressDeviceSessions = new HashMap<>(); // connectionless protocols

    private long findDeviceId(SocketAddress remoteAddress, String... uniqueIds) {
        long deviceId = 0;
        if (uniqueIds.length > 0) {
            try {
                for (String uniqueId : uniqueIds) {
                    if (uniqueId != null) {
                        Device device = Context.getIdentityManager().getByUniqueId(uniqueId);
                        if (device != null) {
                            deviceId = device.getId();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.warning(e);
            }
            if (deviceId == 0) {
                if (Context.getConfig().getBoolean("database.registerUnknown")) {
                    return addUnknownDevice(uniqueIds[0]);
                }

                StringBuilder message = new StringBuilder("Unknown device -");
                for (String uniqueId : uniqueIds) {
                    message.append(" ").append(uniqueId);
                }
                if (remoteAddress != null) {
                    message.append(" (").append(((InetSocketAddress) remoteAddress).getHostString()).append(")");
                }
                Log.warning(message.toString());
            }
        }
        return deviceId;
    }

    public DeviceSession getDeviceSession(Channel channel, SocketAddress remoteAddress, String... uniqueIds) {
        if (channel != null && channel.getPipeline().get(HttpRequestDecoder.class) != null
                || Context.getConfig().getBoolean("decoder.ignoreSessionCache")) {
            long deviceId = findDeviceId(remoteAddress, uniqueIds);
            if (deviceId != 0) {
                if (Context.getConnectionManager() != null) {
                    Context.getConnectionManager().addActiveDevice(deviceId, protocol, channel, remoteAddress);
                }
                return new DeviceSession(deviceId);
            } else {
                return null;
            }
        }
        if (channel instanceof DatagramChannel) {
            long deviceId = findDeviceId(remoteAddress, uniqueIds);
            DeviceSession deviceSession = addressDeviceSessions.get(remoteAddress);
            if (deviceSession != null && (deviceSession.getDeviceId() == deviceId || uniqueIds.length == 0)) {
                return deviceSession;
            } else if (deviceId != 0) {
                deviceSession = new DeviceSession(deviceId);
                addressDeviceSessions.put(remoteAddress, deviceSession);
                if (Context.getConnectionManager() != null) {
                    Context.getConnectionManager().addActiveDevice(deviceId, protocol, channel, remoteAddress);
                }
                return deviceSession;
            } else {
                return null;
            }
        } else {
            if (channelDeviceSession == null) {
                long deviceId = findDeviceId(remoteAddress, uniqueIds);
                if (deviceId != 0) {
                    channelDeviceSession = new DeviceSession(deviceId);
                    if (Context.getConnectionManager() != null) {
                        Context.getConnectionManager().addActiveDevice(deviceId, protocol, channel, remoteAddress);
                    }
                }
            }
            return channelDeviceSession;
        }
    }

    public BaseProtocolDecoder(Protocol protocol) {
        this.protocol = protocol;
    }

    public void getLastLocation(Position position, Date deviceTime) {
        if (position.getDeviceId() != 0) {
            position.setOutdated(true);

            Position last = Context.getIdentityManager().getLastPosition(position.getDeviceId());
            if (last != null) {
                position.setFixTime(last.getFixTime());
                position.setValid(last.getValid());
                position.setLatitude(last.getLatitude());
                position.setLongitude(last.getLongitude());
                position.setAltitude(last.getAltitude());
                position.setSpeed(last.getSpeed());
                position.setCourse(last.getCourse());
                position.setAccuracy(last.getAccuracy());
            } else {
                position.setFixTime(new Date(0));
            }

            if (deviceTime != null) {
                position.setDeviceTime(deviceTime);
            } else {
                position.setDeviceTime(new Date());
            }
        }
    }

    @Override
    protected void onMessageEvent(
            Channel channel, SocketAddress remoteAddress, Object originalMessage, Object decodedMessage) {
        if (Context.getStatisticsManager() != null) {
            Context.getStatisticsManager().registerMessageReceived();
        }
        Position position = null;
        if (decodedMessage != null) {
            if (decodedMessage instanceof Position) {
                position = (Position) decodedMessage;
            } else if (decodedMessage instanceof Collection) {
                Collection positions = (Collection) decodedMessage;
                if (!positions.isEmpty()) {
                    position = (Position) positions.iterator().next();
                }
            }
        }
        if (position != null) {
            Context.getConnectionManager().updateDevice(
                    position.getDeviceId(), Device.STATUS_ONLINE, new Date());
        } else {
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession != null) {
                Context.getConnectionManager().updateDevice(
                        deviceSession.getDeviceId(), Device.STATUS_ONLINE, new Date());
            }
        }
    }

    @Override
    protected Object handleEmptyMessage(Channel channel, SocketAddress remoteAddress, Object msg) {
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (Context.getConfig().getBoolean("database.saveEmpty") && deviceSession != null) {
            Position position = new Position();
            position.setProtocol(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            getLastLocation(position, null);
            return position;
        } else {
            return null;
        }
    }

}
