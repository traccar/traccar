package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.CellTower;
import org.traccar.model.Network;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import java.net.SocketAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TranSyncProtocolDecoder extends BaseProtocolDecoder {

    public TranSyncProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    public Date getDatefromIntegerParameters(int year, int month, int day, int hours, int minutes, int seconds) {

        String dataString = ""+year + "-" + month + "-" + day + " " + hours + ":" + minutes + ":" + seconds;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return simpleDateFormat.parse(dataString);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void setDeviceAlert(Position position, int alert) {
        switch (alert) {
            case 10:
                position.set(Position.ALARM_SOS, true);
                break;
            case 11:
                position.set(Position.ALARM_SOS, false);
                break;
            case 16:
                position.set(Position.KEY_EVENT, 16);
                break;
            case 3:
                position.set("distressCutOff", true);
                break;
            case 22:
                position.set("tilt", true);
                break;
            case 9:
                position.set(Position.KEY_EVENT, 9);
            case 17:
                position.set(Position.ALARM_OVERSPEED, true);
                break;
            case 13:
                position.set(Position.ALARM_BRAKING, true);
                break;
            case 14:
                position.set(Position.ALARM_ACCELERATION, true);
                break;
            case 15:
                position.set(Position.KEY_EVENT, 15);
                break;
            case 23:
                position.set(Position.ALARM_ACCIDENT, true);
                break;
            case 12:
                position.set(Position.KEY_EVENT, 12);
                break;
            case 6:
                position.set(Position.ALARM_POWER_RESTORED, true);
                break;
            case 4:
                position.set(Position.ALARM_LOW_BATTERY, true);
                break;
            case 5:
                position.set(Position.KEY_EVENT, 5);
                break;

        }

    }

    private void setNetwork(Position position, int lacInt, int cellIdInt, int mobileNetworkCode, int gsmSignalStrength){
        CellTower cellTower = CellTower.fromLacCid(getConfig(), lacInt, cellIdInt);
        cellTower.setMobileNetworkCode(mobileNetworkCode);
        cellTower.setSignalStrength(gsmSignalStrength);
        position.setNetwork(new Network(cellTower));

    }

    private String getTrackerModel(int mask){

        switch (mask) {
            case 1:
                return  "basic";
            case 2:
                return "asset";
            case 3:
                return "bike";

            case 4:
                return "serial";

            case 5:
                return "obd";
            case 6:
                return "l1";
            case 7:
                return "ais";
            default:
                return "unknown";
        }

    }

    private Position parseData(Channel channel, SocketAddress remoteAddress, ByteBuf buf) {
        if (ByteBufUtil.hexDump(buf, 0, 2).equalsIgnoreCase("3A3A")) {
            buf.readUnsignedShort();
        }
        int packetLength = buf.readByte();
        int lacInt = buf.readUnsignedShort();
        String deviceId = ByteBufUtil.hexDump(buf, buf.readerIndex(), 8).toUpperCase().trim().replaceFirst("^0+(?!$)", "");
        buf.readBytes(8);
        int informationSerialNumber = buf.readUnsignedShort();
        int protocolNumber = buf.readUnsignedByte();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, deviceId);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        long devicesessionId = deviceSession.getDeviceId();

        position.setDeviceId(devicesessionId) ;
        position.setValid(true);
        int year =  buf.readUnsignedByte();
        int month = buf.readUnsignedByte();
        int day =   buf.readUnsignedByte();
        int hour =  buf.readUnsignedByte();
        int minute = buf.readUnsignedByte();
        int second = buf.readUnsignedByte();
        position.setTime(getDatefromIntegerParameters(year, month, day, hour, minute, second));
        position.setLatitude(buf.readUnsignedInt() / 1800000.0);
        position.setLongitude(buf.readUnsignedInt() / 1800000.0);
        position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
        position.setCourse(buf.readUnsignedShort());
        int mobileNetworkCode = buf.readUnsignedByte();
        int cellIdInt = buf.readUnsignedShort();
        int zeroByte =  buf.readUnsignedByte();
        position.set(Position.PREFIX_OUT + 1, BitUtil.check(zeroByte, 0));
        position.set(Position.PREFIX_OUT + 2, BitUtil.check(zeroByte, 1));
        position.set(Position.PREFIX_IN + 3, BitUtil.check(zeroByte, 2));
        if (BitUtil.check(zeroByte, 3)) position.set(Position.ALARM_POWER_OFF, true);
        position.set(Position.KEY_IGNITION, BitUtil.check(zeroByte, 4));
        position.set("gpsFix", BitUtil.check(zeroByte, 7));
        int oneByte =  buf.readUnsignedByte();
        int deviceAlert =  buf.readUnsignedByte();
        if (deviceAlert > 0) { setDeviceAlert(position, deviceAlert); }
        int threeByte =  buf.readUnsignedByte();
        if (!((threeByte & 0b10000000) == 0)) position.set("restored", true);
        if ((threeByte & 0b00100000) != 0) {
            position.set(Position.ALARM_GPS_ANTENNA_CUT, true);
            position.set("gpsAlert", true);
        }
        position.set("model", getTrackerModel(threeByte & 0b00001111));
        int gsmSignalStrength = buf.readUnsignedByte();
        position.set(Position.KEY_BATTERY, (double) (buf.readUnsignedByte() / 10));
        position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
        position.set(Position.KEY_HDOP, buf.readUnsignedByte());
        position.set(Position.PREFIX_ADC + 1, (short) buf.readUnsignedShort());
        boolean isOptionalParameters = (buf.readableBytes() > 2);
        setNetwork(position, lacInt, cellIdInt, mobileNetworkCode, gsmSignalStrength);
        if (isOptionalParameters) { //  Always True
            int odometerIndex = buf.readUnsignedByte();
            int odometerLength = buf.readUnsignedByte();
            if (odometerLength > 0) {
                String odometerReading = ByteBufUtil.hexDump(buf, buf.readerIndex(), odometerLength).toUpperCase().trim().replaceFirst("^0+(?!$)", "");
                int odometer = Integer.parseInt(odometerReading);
                buf.readBytes(odometerLength);
                position.set(Position.KEY_ODOMETER, odometer);
            }
            if ((buf.readableBytes() > 2)) {
                int rfidIndex = buf.readUnsignedByte();
                int rfidLength = buf.readUnsignedByte();
                if(rfidLength > 0) {
                    String rfidTagName = ByteBufUtil.hexDump(buf, buf.readerIndex(), rfidLength).toUpperCase().trim();
                    buf.readBytes(rfidLength);
                    position.set("tag", rfidTagName);
                }
            }
            if ((buf.readableBytes() > 5)) {
                int adcTwoIndex =  buf.readUnsignedByte();
                int adcTwoLength =  buf.readUnsignedByte();
                if (adcTwoLength > 0){
                    position.set(Position.PREFIX_ADC + 2, buf.readUnsignedShort());
                }
            }
        }

        return position;
    }

    @Override
    protected Object decode( Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        ByteBuf buffer = (ByteBuf) msg;
        return parseData(channel, remoteAddress, buffer);
    }

}
