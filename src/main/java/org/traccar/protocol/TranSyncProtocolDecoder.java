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

    private boolean isOptionalParameters;
    private boolean extended;
    private final String STX = "3A3A";
    private final String ETX = "2323";
    private String lac;
    private String deviceId;
    private Date datePacket;
    private double latitude;
    private double longitude;
    private double speed;
    private int course;
    private int mobileNetworkCode;
    private int gsmSignalStrength;
    private double batteryVoltage;
    private int satellitesNumber;
    private int hdopProtocol;
    private short adcVoltageInMilliVolts;
    private String rfidTagName;
    private int adc2;
    private int cellIdInt;
    private int lacInt;
    private int odometer;
    private boolean isGpsFix;
    private boolean isLiveData;
    private boolean hasGpsAlert;
    private boolean isIgnitionOff;
    private boolean isPowerOff;
    private int deviceAlert;
    private String gpsTrackerModel;
    private boolean isOutPutTwo;
    private boolean isOutPutOne;
    private boolean isInPutThree;



    public String getLac() {
        return lac;
    }

    public void setLac(String lac) {
        this.lac = lac;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setDatePacket(Date datePacket) {
        this.datePacket = datePacket;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getCourse() {
        return course;
    }

    public void setCourse(int course) {
        this.course = course;
    }

    public void setCellId(String cellId) {
    }

    public int getAdc2() {
        return adc2;
    }

    public void setAdc2(String adc2) {
        this.adc2 = Integer.parseInt(adc2);
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setTimestampPacket(int year, int month, int day, int hours, int minutes, int seconds) {

        String dataString = ""+year + "-" + month + "-" + day + " " + hours + ":" + minutes + ":" + seconds;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date deviceTime = simpleDateFormat.parse(dataString);
            long timestampPacket = deviceTime.toInstant().toEpochMilli();
            this.setDatePacket(deviceTime);

        } catch (ParseException e) {
            e.printStackTrace();
        }
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

    protected void setInstanceParameters(Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        if (ByteBufUtil.hexDump(buf, 0, 2).toUpperCase().equals(this.STX)) {
            buf.readUnsignedShort();
        }
        int packetLength = buf.readByte();
        this.lacInt = buf.readUnsignedShort();
        this.deviceId = ByteBufUtil.hexDump(buf, buf.readerIndex(), 8).toUpperCase().trim().replaceFirst("^0+(?!$)", "");
        buf.readBytes(8);
        int informationSerialNumber = buf.readUnsignedShort();
        int protocolNumber = buf.readUnsignedByte();
        // Information Content
        int year =  buf.readUnsignedByte();
        int month = buf.readUnsignedByte();
        int day =   buf.readUnsignedByte();
        int hour =  buf.readUnsignedByte();
        int minute = buf.readUnsignedByte();
        int second = buf.readUnsignedByte();
        this.setTimestampPacket(year, month, day, hour, minute, second);
        this.latitude = (long) buf.readUnsignedInt() / 1800000.0;
        this.longitude = (long) buf.readUnsignedInt() / 1800000.0;
        this.speed = UnitsConverter.knotsFromKph(buf.readUnsignedByte());
        this.course = buf.readUnsignedShort();
        this.mobileNetworkCode = buf.readUnsignedByte();
        this.cellIdInt = buf.readUnsignedShort();
        // Status Bytes
        int zeroByte =  buf.readUnsignedByte();
        this.isOutPutTwo = BitUtil.check(zeroByte, 0);
        this.isOutPutOne = BitUtil.check(zeroByte, 1);
        this.isInPutThree = BitUtil.check(zeroByte, 2);
        this.isPowerOff = BitUtil.check(zeroByte, 3);
        this.isIgnitionOff = BitUtil.check(zeroByte, 4);
        boolean isgpsFixed = BitUtil.check(zeroByte, 7);


        this.isGpsFix = BitUtil.check(zeroByte, 0);
        int oneByte =  buf.readUnsignedByte();
        this.deviceAlert =  buf.readUnsignedByte();
        int threeByte =  buf.readUnsignedByte();
        this.isLiveData = (threeByte & 0b10000000) == 0;
        this.hasGpsAlert = (threeByte & 0b00100000) != 0;
        int trackerType = threeByte & 0b00001111;

        switch (trackerType) {
            case 1:
                this.gpsTrackerModel = "basic";
                break;
            case 2:
                this.gpsTrackerModel = "asset";
                break;
            case 3:
                this.gpsTrackerModel = "bike";
                break;
            case 4:
                this.gpsTrackerModel = "serial";
                break;
            case 5:
                this.gpsTrackerModel = "obd";
                break;
            case 6:
                this.gpsTrackerModel = "l1";
                break;
            case 7:
                this.gpsTrackerModel = "ais";
                break;
            default:
                this.gpsTrackerModel = "unknown";
                break;
        }
        // Additional Info
        this.gsmSignalStrength = buf.readUnsignedByte();
        this.batteryVoltage = (double) (buf.readUnsignedByte() / 10);
        this.satellitesNumber = buf.readUnsignedByte();
        this.hdopProtocol = buf.readUnsignedByte();
        this.adcVoltageInMilliVolts = (short) buf.readUnsignedShort();
        this.isOptionalParameters = (boolean) (buf.readableBytes() > 2);
        //  Optional parameters
        if (this.isOptionalParameters) { //  Always True
            int odometerIndex = buf.readUnsignedByte();
            int odometerLength = buf.readUnsignedByte();
            if (odometerLength > 0) {
                String odometerReading = ByteBufUtil.hexDump(buf, buf.readerIndex(), odometerLength).toUpperCase().trim().replaceFirst("^0+(?!$)", "");
                this.odometer = Integer.parseInt(odometerReading);
                buf.readBytes(odometerLength);
            }
            if ((buf.readableBytes() > 2)) {
                int rfidIndex = buf.readUnsignedByte();
                int rfidLength = buf.readUnsignedByte();
                if(rfidLength > 0) {
                    this.rfidTagName = ByteBufUtil.hexDump(buf, buf.readerIndex(), rfidLength).toUpperCase().trim();
                    buf.readBytes(rfidLength);
                }
            }
            if ((buf.readableBytes() > 5)) {
                int adcTwoIndex =  buf.readUnsignedByte();
                int adcTwoLength =  buf.readUnsignedByte();
                if (adcTwoLength > 0){
                    this.adc2 = buf.readUnsignedShort();
                }
            }
        }

    }

    private Position positionHandler(Channel channel, SocketAddress remoteAddress) {

        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, this.deviceId);
        if (deviceSession == null) {
            return null;
        }
        Position position = new Position(getProtocolName());
        long devicesessionId = deviceSession.getDeviceId();
        position.setDeviceId(devicesessionId) ;
        position.setValid(true);
        position.setTime(this.datePacket);
        position.setLatitude(this.latitude);
        position.setLongitude(this.longitude);
        position.setSpeed(this.speed);
        position.setCourse(this.course);
        position.set("model", this.gpsTrackerModel);
        position.set(Position.KEY_HDOP, this.hdopProtocol);
        position.set(Position.KEY_BATTERY, this.batteryVoltage);
        position.set(Position.KEY_ODOMETER, this.odometer);
        position.set(Position.PREFIX_ADC + 1, this.adcVoltageInMilliVolts);
        position.set(Position.PREFIX_ADC + 2, this.adc2);
        position.set("tag", this.rfidTagName);
        position.set(Position.KEY_SATELLITES, this.satellitesNumber);
        CellTower cellTower = CellTower.fromLacCid(getConfig(), this.lacInt, this.cellIdInt);
        cellTower.setMobileNetworkCode(this.mobileNetworkCode);
        cellTower.setSignalStrength(this.gsmSignalStrength);
        position.setNetwork(new Network(cellTower));
        if (this.isOutPutOne){ position.set(Position.PREFIX_OUT + 1, this.isOutPutOne);}
        if (this.isOutPutTwo){ position.set(Position.PREFIX_OUT + 2, this.isOutPutTwo);}
        if (this.isInPutThree){ position.set(Position.PREFIX_IN + 2, this.isInPutThree);}
        if (this.isGpsFix) { position.set("gpsFix", this.isGpsFix); }
        if (!this.isLiveData) { position.set("restored", true); }
        if (this.hasGpsAlert) {
            position.set(Position.ALARM_GPS_ANTENNA_CUT, true);
            position.set("gpsAlert", true);
        }
        position.set(Position.KEY_IGNITION, this.isIgnitionOff);
        if (isPowerOff){ position.set(Position.ALARM_POWER_OFF, this.isPowerOff );}
        position.set(Position.KEY_GPS, true);
        if (this.deviceAlert > 0) { setDeviceAlert(position, this.deviceAlert); }

        return position;
    }

    @Override
    protected Object decode( Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        try {
            setInstanceParameters(msg);
        }
        catch (Exception e) {
            return null;
        }

        return positionHandler(channel, remoteAddress);
    }

}
