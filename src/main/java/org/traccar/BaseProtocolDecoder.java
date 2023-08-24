/*
 * Copyright 2012 - 2022 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.traccar.config.Keys;
import org.traccar.database.CommandsManager;
import org.traccar.database.MediaManager;
import org.traccar.database.StatisticsManager;
import org.traccar.helper.UnitsConverter;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.ConnectionManager;
import org.traccar.session.DeviceSession;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.StorageException;

import jakarta.inject.Inject;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

public abstract class BaseProtocolDecoder extends ExtendedObjectDecoder {

    private static final String PROTOCOL_UNKNOWN = "unknown";

    private final Protocol protocol;

    private CacheManager cacheManager;
    private ConnectionManager connectionManager;
    private StatisticsManager statisticsManager;
    private MediaManager mediaManager;
    private CommandsManager commandsManager;

    public BaseProtocolDecoder(Protocol protocol) {
        this.protocol = protocol;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    @Inject
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Inject
    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Inject
    public void setStatisticsManager(StatisticsManager statisticsManager) {
        this.statisticsManager = statisticsManager;
    }

    @Inject
    public void setMediaManager(MediaManager mediaManager) {
        this.mediaManager = mediaManager;
    }

    @Inject
    public void setCommandsManager(CommandsManager commandsManager) {
        this.commandsManager = commandsManager;
    }

    public CommandsManager getCommandsManager() {
        return commandsManager;
    }

    public String writeMediaFile(String uniqueId, ByteBuf buf, String extension) {
        return mediaManager.writeFile(uniqueId, buf, extension);
    }

    public String getProtocolName() {
        return protocol != null ? protocol.getName() : PROTOCOL_UNKNOWN;
    }

    public String getServer(Channel channel, char delimiter) {
        String server = getConfig().getString(Keys.PROTOCOL_SERVER.withPrefix(getProtocolName()));
        if (server == null && channel != null) {
            InetSocketAddress address = (InetSocketAddress) channel.localAddress();
            server = address.getAddress().getHostAddress() + ":" + address.getPort();
        }
        return server != null ? server.replace(':', delimiter) : null;
    }

    protected double convertSpeed(double value, String defaultUnits) {
        switch (getConfig().getString(getProtocolName() + ".speed", defaultUnits)) {
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

    protected TimeZone getTimeZone(long deviceId) {
        return getTimeZone(deviceId, "UTC");
    }

    protected TimeZone getTimeZone(long deviceId, String defaultTimeZone) {
        TimeZone result = TimeZone.getTimeZone(defaultTimeZone);
        String timeZoneName = AttributeUtil.lookup(cacheManager, Keys.DECODER_TIMEZONE, deviceId);
        if (timeZoneName != null) {
            result = TimeZone.getTimeZone(timeZoneName);
        }
        return result;
    }

    public DeviceSession getDeviceSession(Channel channel, SocketAddress remoteAddress, String... uniqueIds) {
        try {
            return connectionManager.getDeviceSession(protocol, channel, remoteAddress, uniqueIds);
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    public void getLastLocation(Position position, Date deviceTime) {
        if (position.getDeviceId() != 0) {
            position.setOutdated(true);

            Position last = cacheManager.getPosition(position.getDeviceId());
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
        if (statisticsManager != null) {
            statisticsManager.registerMessageReceived();
        }
        Set<Long> deviceIds = new HashSet<>();
        if (decodedMessage != null) {
            if (decodedMessage instanceof Position) {
                deviceIds.add(((Position) decodedMessage).getDeviceId());
            } else if (decodedMessage instanceof Collection) {
                Collection<Position> positions = (Collection) decodedMessage;
                for (Position position : positions) {
                    deviceIds.add(position.getDeviceId());
                }
            }
        }
        if (deviceIds.isEmpty()) {
            DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
            if (deviceSession != null) {
                deviceIds.add(deviceSession.getDeviceId());
            }
        }
        for (long deviceId : deviceIds) {
            connectionManager.updateDevice(deviceId, Device.STATUS_ONLINE, new Date());
            sendQueuedCommands(channel, remoteAddress, deviceId);
        }
    }

    protected void sendQueuedCommands(Channel channel, SocketAddress remoteAddress, long deviceId) {
        for (Command command : commandsManager.readQueuedCommands(deviceId)) {
            protocol.sendDataCommand(channel, remoteAddress, command);
        }
    }

    @Override
    protected Object handleEmptyMessage(Channel channel, SocketAddress remoteAddress, Object msg) {
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress);
        if (getConfig().getBoolean(Keys.DATABASE_SAVE_EMPTY) && deviceSession != null) {
            Position position = new Position(getProtocolName());
            position.setDeviceId(deviceSession.getDeviceId());
            getLastLocation(position, null);
            return position;
        } else {
            return null;
        }
    }

}
