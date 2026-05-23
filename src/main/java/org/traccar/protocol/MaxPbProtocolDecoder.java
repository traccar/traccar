/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.model.Position;
import org.traccar.protobuf.maxpb.MaxPbMessage;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MaxPbProtocolDecoder extends BaseProtocolDecoder {

    public MaxPbProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public static final int MSG_POSITION = 0x0001;
    public static final int MSG_ACK = 0x0002;
    public static final int MSG_STATUS = 0x0011;

    private DeviceSession getDeviceSession(
            Channel channel, SocketAddress remoteAddress, long deviceId, MaxPbMessage.IDPack idPack) {
        return idPack.hasImei()
                ? getDeviceSession(channel, remoteAddress, idPack.getImei(), String.valueOf(deviceId))
                : getDeviceSession(channel, remoteAddress, String.valueOf(deviceId));
    }

    private Position decodeReport(DeviceSession deviceSession, MaxPbMessage.ReportData report) {

        if (!report.hasPositionInfo()) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setTime(report.hasDateTime() ? new Date(report.getDateTime() * 1000L) : new Date());

        MaxPbMessage.PositionInfo positionInfo = report.getPositionInfo();
        position.setLatitude(positionInfo.getLatitude() / 10000000.0);
        position.setLongitude(positionInfo.getLongitude() / 10000000.0);
        position.setAltitude(positionInfo.getAltitude());
        position.setCourse(positionInfo.getDirectionDegrees());
        position.setAccuracy(positionInfo.getEstimatedPositionError());

        if (report.hasGps()) {
            MaxPbMessage.GpsInfo gps = report.getGps();
            position.setValid(gps.getFixState() >= 2);
            position.set(Position.KEY_SATELLITES, gps.getSvn());
            position.set(Position.KEY_HDOP, gps.getHdop() / 10.0);
        } else {
            position.setValid(true);
        }

        if (report.hasIos()) {
            position.set(Position.KEY_IGNITION, report.getIos().getIgnitionState() != 0);
        }

        if (report.hasFlags() && report.getFlags().hasDeviceInfo()) {
            MaxPbMessage.DeviceInfo deviceInfo = report.getFlags().getDeviceInfo();
            position.set(Position.KEY_POWER, deviceInfo.getExtPowerValue() / 1000.0);
            position.set(Position.KEY_BATTERY, deviceInfo.getBattValue() / 1000.0);
            position.set(Position.KEY_BATTERY_LEVEL, deviceInfo.getBattPercent());
        }

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ByteBuf buf = (ByteBuf) msg;

        buf.skipBytes(4); // header
        buf.readUnsignedShortLE(); // size
        buf.readUnsignedShortLE(); // crc
        int type = buf.readUnsignedShortLE();
        int format = buf.readUnsignedShortLE();
        if (format != 0) {
            return null;
        }

        byte[] data = ByteBufUtil.getBytes(buf);

        if (type == MSG_POSITION) {

            MaxPbMessage.MultipleReportData message = MaxPbMessage.MultipleReportData.parseFrom(data);
            DeviceSession deviceSession = getDeviceSession(
                    channel, remoteAddress, message.getDeviceID(), message.getIdentificationPack());
            if (deviceSession == null) {
                return null;
            }

            List<Position> positions = new ArrayList<>();
            for (MaxPbMessage.ReportData report : message.getOldReportDataList()) {
                Position position = decodeReport(deviceSession, report);
                if (position != null) {
                    positions.add(position);
                }
            }
            if (message.hasNewReportData()) {
                Position position = decodeReport(deviceSession, message.getNewReportData());
                if (position != null) {
                    positions.add(position);
                }
            }
            return positions.isEmpty() ? null : positions;

        } else if (type == MSG_STATUS) {

            MaxPbMessage.ReportStatus message = MaxPbMessage.ReportStatus.parseFrom(data);
            getDeviceSession(channel, remoteAddress, message.getDeviceID(), message.getIdentificationPack());

        }

        return null;
    }

}
