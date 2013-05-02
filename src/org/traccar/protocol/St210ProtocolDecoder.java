package org.traccar.protocol;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.Log;
import org.traccar.model.DataManager;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class St210ProtocolDecoder extends BaseProtocolDecoder {


    public St210ProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }

    private enum ST210FIELDS {
                HDR_STATUS("SA200STT;","Status Report"),
                HDR_EMERGENCY("SA200EMG;","Emergency Report"),
                HDR_EVENT("SA200EVT;", "Event Report"),
                HDR_ALERT("SA200ALT;","Alert Report"),
                HDR_ALIVE("SA200ALV;","Alive Report"),
                DEV_ID("(\\d+);", "Device ID"),
                SW_VER("(\\d{3});", "Software Release Version"),
                DATE("(\\d+);","GPS date (yyyymmdd) Year + Month + Day"),
                TIME("(\\d{2}:\\d{2}:\\d{2});","GPS time (hh:mm:ss) Hour : Minute : Second"),
                CELL("(\\w+);","Location Code ID (3 digits hex) + Serving Cell BSIC(2 digits decimal)"),
                LAT("(-\\d{2}.\\d+);", "Latitude (+/-xx.xxxxxx)"),
                LON("(-\\d{3}.\\d+);", "Longitude (+/-xxx.xxxxxx)"),
                SPD("(\\d{3}.\\d{3});","Speed in km/h - This value returns to 0 when it is over than 200,000Km"),
                CRS("(\\d{3}.\\d{2});", "Course over ground in degree"),
                SATT("(\\d+);", "Number of satellites"),
                FIX("(\\d);","GPS is fixed(1)\n" + "GPS is not fixed(0)"),
                DIST("(\\d+);","Traveled ddistance in meter"),
                PWR_VOLT("(\\d+.\\d{2});","Voltage value of main power"),
                IO("(\\d+);","Current I/O status of inputs and outputs."),
                MODE("(\\d);","1 = Idle mode (Parking)\n" + "2 = Active Mode (Driving)"),
                MSG_NUM("(\\d{4});","Message number - After 9999 is reported, message number returns to 0000"),
                EMG_ID("(\\d);", "Emergency type"),
                EVT_ID("(\\d);", "Event type"),
                ALERT_ID("(\\d+);", "Alert type");

        private String pattern;

        private String desc;

        private ST210FIELDS(String pattern, String desc) {
            this.pattern = pattern;
            this.desc = desc;
        }

        public String getPattern() {
            return pattern;
        }

        public String getDesc() {
            return desc;
        }

        public void populatePosition(Position position, String groupValue,
                DataManager dataManager) throws Exception {

            switch (this) {

            case DEV_ID:
                position.setDeviceId(dataManager.getDeviceByImei(groupValue)
                        .getId());
                break;

            case LAT:
                position.setLatitude(Double.valueOf(groupValue));
                break;

            case LON:
                position.setLongitude(Double.valueOf(groupValue));
                break;

            case CRS:
                position.setCourse(Double.valueOf(groupValue));
                break;

            case PWR_VOLT:
                position.setPower(Double.valueOf(groupValue));
                break;

            case SPD:
                position.setSpeed(Double.valueOf(groupValue));
                break;

            case MODE:
                //position.setMode(Integer.parseInt(groupValue));
                break;

            case DATE: {
                // Date
                Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                time.clear();
                time.set(Calendar.YEAR, Integer.valueOf(Integer.valueOf(groupValue.substring(0, 4))));
                time.set(Calendar.MONTH, Integer.valueOf(Integer.valueOf(groupValue.substring(4, 6))-1));
                time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(Integer.valueOf(groupValue.substring(6, 8))));

                /*Calendar ret = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

                ret.setTimeInMillis(time.getTimeInMillis() +
                                    TimeZone.getTimeZone("UTC").getOffset(time.getTimeInMillis()) -
                					TimeZone.getDefault().getOffset(time.getTimeInMillis()));*/

                position.setTime(time.getTime());

                break;
            }

            case TIME: {

                Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                time.clear();
                time.setTime(position.getTime());

                time.set(Calendar.HOUR_OF_DAY, Integer.valueOf(Integer.valueOf(groupValue.substring(0, 2))));
                time.set(Calendar.MINUTE, Integer.valueOf(Integer.valueOf(groupValue.substring(3, 5))));
                time.set(Calendar.SECOND, Integer.valueOf(Integer.valueOf(groupValue.substring(6, 8))));

                /*Calendar ret = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

                ret.setTimeInMillis(time.getTimeInMillis() +
                                    TimeZone.getTimeZone("UTC").getOffset(time.getTimeInMillis()) -
                					TimeZone.getDefault().getOffset(time.getTimeInMillis()));*/

                position.setTime(time.getTime());

                break;
            }

            default:
                break;
            }

        }
    }

    private enum FIELD_FIX_VALUE {
        FIXED(1, "GPS is fixed"), NOT_FIXED(0, "GPS is not fixed");

        private int value;

        private String desc;

        private FIELD_FIX_VALUE(int value, String desc) {
            this.value = value;
            this.desc = desc;
        }

        public int getValue() {
            return value;
        }

        public String getDesc() {
            return desc;
        }

        public FIELD_FIX_VALUE getValueOf(String indiceStr) {
            int indice = Integer.valueOf(indiceStr);
            return getValueOf(indice);
        }

        public FIELD_FIX_VALUE getValueOf(int indice) {
            switch (indice) {
            case 1:
                return FIXED;
            case 0:
                return NOT_FIXED;
            default:
                throw new IllegalArgumentException("Index not defined");
            }
        }
    }

    private enum FIELD_MODE_VALUE {
        PARKING(1, "Idle mode (Parking)"), DRIVING(2, "Active Mode (Driving)");

        private int value;

        private String desc;

        private FIELD_MODE_VALUE(int value, String desc) {
            this.value = value;
            this.desc = desc;
        }

        public int getValue() {
            return value;
        }

        public String getDesc() {
            return desc;
        }

        public FIELD_MODE_VALUE getValueOf(String indiceStr) {
            int indice = Integer.valueOf(indiceStr);
            return getValueOf(indice);
        }

        public FIELD_MODE_VALUE getValueOf(int indice) {
            switch (indice) {
            case 1:
                return PARKING;
            case 2:
                return DRIVING;
            default:
                throw new IllegalArgumentException("Index not defined");
            }
        }
    }

    private enum FIELD_EMG_ID_VALUE {
        PANIC(1, "Emergency by panic button"),
        PARKING(2,"Emergency by parking lock"),
        MAIN_POWER(3,"Emergency by removing main power"),
        ANTI_THEFT(5,"Emergency by anti-theft");

        private int value;

        private String desc;

        private FIELD_EMG_ID_VALUE(int value, String desc) {
            this.value = value;
            this.desc = desc;
        }

        public int getValue() {
            return value;
        }

        public String getDesc() {
            return desc;
        }

        public FIELD_EMG_ID_VALUE getValueOf(String indiceStr) {
            int indice = Integer.valueOf(indiceStr);
            return getValueOf(indice);
        }

        public FIELD_EMG_ID_VALUE getValueOf(int indice) {
            switch (indice) {
            case 1:
                return PANIC;
            case 2:
                return PARKING;
            case 3:
                return MAIN_POWER;
            case 5:
                return ANTI_THEFT;
            default:
                throw new IllegalArgumentException("Index not defined");
            }
        }
    }

    private enum FIELD_EVT_ID_VALUE {
        INPUT1_GROUND(1, "Input1 goes to ground state"),
        INPUT1_OPEN(2,"Input1 goes to open state"),
        INPUT2_GROUND(3,"Input2 goes to ground state"),
        INPUT2_OPEN(4,"Input2 goes to open state"),
        INPUT3_GROUND(5,"Input3 goes to ground state"),
        INPUT3_OPEN(6,"Input3 goes to open state");

        private int value;

        private String desc;

        private FIELD_EVT_ID_VALUE(int value, String desc) {
            this.value = value;
            this.desc = desc;
        }

        public int getValue() {
            return value;
        }

        public String getDesc() {
            return desc;
        }

        public FIELD_EVT_ID_VALUE getValueOf(String indiceStr) {
            int indice = Integer.valueOf(indiceStr);
            return getValueOf(indice);
        }

        public FIELD_EVT_ID_VALUE getValueOf(int indice) {
            switch (indice) {
            case 1:
                return INPUT1_GROUND;
            case 2:
                return INPUT1_OPEN;
            case 3:
                return INPUT2_GROUND;
            case 4:
                return INPUT2_OPEN;
            case 5:
                return INPUT3_GROUND;
            case 6:
                return INPUT3_OPEN;
            default:
                throw new IllegalArgumentException("Index not defined");
            }
        }
    }

    private enum FIELD_ALERT_ID_VALUE {
        DRIVING_FASTER(1, "Start driving faster than SPEED_LIMIT"),
        OVER_SPPED(2, "Ended over speed condition"),
        DISCON_GPS(3,"Disconnected GPS antenna"),
        RECON_GPS(4,"Reconnected GPS antenna after disconnected"),
        OUT_GEO_FENCE(5,"The vehicle went out from the geo-fence that has following ID"),
        INTO_GEO_FENCE(6,"The vehicle entered into the geo-fence that has following ID"),
        SHORTED_GPS(8, "Shorted GPS antenna"),
        DEEP_SLEEP_ON(9,"Enter to deep sleep mode"),
        DEEP_SLEEP_OFF(10,"Exite from deep sleep mode"),
        BKP_BATTERY(13,"Backup battery error"),
        BATTERY_DOWN(14,"Vehicle battery goes down to so low level"),
        SHOCKED(15,"Shocked"),
        COLLISION(16, "Occurred some collision"),
        DEVIATE_ROUT(18, "Deviate from predefined rout"),
        ENTER_ROUT(19,"Enter into predefined rout");

        private int value;

        private String desc;

        private FIELD_ALERT_ID_VALUE(int value, String desc) {
            this.value = value;
            this.desc = desc;
        }

        public int getValue() {
            return value;
        }

        public String getDesc() {
            return desc;
        }

        public FIELD_ALERT_ID_VALUE getValueOf(String indiceStr) {
            int indice = Integer.valueOf(indiceStr);
            return getValueOf(indice);
        }

        public FIELD_ALERT_ID_VALUE getValueOf(int indice) {
            switch (indice) {
            case 1:
                return DRIVING_FASTER;
            case 2:
                return OVER_SPPED;
            case 3:
                return DISCON_GPS;
            case 4:
                return RECON_GPS;
            case 5:
                return OUT_GEO_FENCE;
            case 6:
                return INTO_GEO_FENCE;
            case 8:
                return SHORTED_GPS;
            case 9:
                return DEEP_SLEEP_ON;
            case 10:
                return DEEP_SLEEP_OFF;
            case 13:
                return BKP_BATTERY;
            case 14:
                return BATTERY_DOWN;
            case 15:
                return SHOCKED;
            case 16:
                return COLLISION;
            case 18:
                return DEVIATE_ROUT;
            case 19:
                return ENTER_ROUT;
            default:
                throw new IllegalArgumentException("Index not defined");
            }
        }
    }

    private enum ST210REPORTS {

        STATUS("SA200STT"), EMERGENCY("SA200EMG"), EVENT("SA200EVT"), ALERT(
                "SA200ALT"), ALIVE("SA200ALV");

        private String header;

        private ST210REPORTS(String header) {
            this.header = header;
        }

        public String getHeader() {
            return this.header;
        }

        public List<ST210FIELDS> getProtocol() {

            if (this.equals(STATUS)) {
                return StatusProtocol;
            }

            if (this.equals(EMERGENCY)) {
                return EmergencyProtocol;
            }

            if (this.equals(EVENT)) {
                return EventProtocol;
            }

            if (this.equals(ALERT)) {
                return AlertProtocol;
            }

            if (this.equals(ALIVE)) {
                return AliveProtocol;
            }

            return null;
        }

        public Pattern getProtocolPattern() {

            if (this.equals(STATUS)) {
                return getPattern(StatusProtocol);
            }

            if (this.equals(EMERGENCY)) {
                return getPattern(EmergencyProtocol);
            }

            if (this.equals(EVENT)) {
                return getPattern(EventProtocol);
            }

            if (this.equals(ALERT)) {
                return getPattern(AlertProtocol);
            }

            if (this.equals(ALIVE)) {
                return getPattern(AliveProtocol);
            }

            return null;
        }

    }

    private static ST210REPORTS getReportType(String msg) {

        if (msg.startsWith(ST210REPORTS.STATUS.getHeader())) {
            return ST210REPORTS.STATUS;
        }

        if (msg.startsWith(ST210REPORTS.EMERGENCY.getHeader())) {
            return ST210REPORTS.EMERGENCY;
        }

        if (msg.startsWith(ST210REPORTS.EVENT.getHeader())) {
            return ST210REPORTS.EVENT;
        }

        if (msg.startsWith(ST210REPORTS.ALERT.getHeader())) {
            return ST210REPORTS.ALERT;
        }

        if (msg.startsWith(ST210REPORTS.ALIVE.getHeader())) {
            return ST210REPORTS.ALIVE;
        }

        return null;
    }

    private static Pattern getPattern(List<ST210FIELDS> protocol) {

        String patternStr = "";

        for (ST210FIELDS field : protocol) {
            patternStr += field.getPattern();
        }

        if(patternStr.endsWith(";")){
            patternStr = patternStr.substring(0, patternStr.length()-1);
        }

        return Pattern.compile(patternStr);

    }

    @SuppressWarnings("serial")
    static private List<ST210FIELDS> StatusProtocol = new LinkedList<ST210FIELDS>() {

        {
            add(ST210FIELDS.HDR_STATUS);
            add(ST210FIELDS.DEV_ID);
            add(ST210FIELDS.SW_VER);
            add(ST210FIELDS.DATE);
            add(ST210FIELDS.TIME);
            add(ST210FIELDS.CELL);
            add(ST210FIELDS.LAT);
            add(ST210FIELDS.LON);
            add(ST210FIELDS.SPD);
            add(ST210FIELDS.CRS);
            add(ST210FIELDS.SATT);
            add(ST210FIELDS.FIX);
            add(ST210FIELDS.DIST);
            add(ST210FIELDS.PWR_VOLT);
            add(ST210FIELDS.IO);
            add(ST210FIELDS.MODE);
            add(ST210FIELDS.MSG_NUM);
        }

    };

    @SuppressWarnings("serial")
    static private List<ST210FIELDS> EmergencyProtocol = new LinkedList<ST210FIELDS>() {

        {
            add(ST210FIELDS.HDR_EMERGENCY);
            add(ST210FIELDS.DEV_ID);
            add(ST210FIELDS.SW_VER);
            add(ST210FIELDS.DATE);
            add(ST210FIELDS.TIME);
            add(ST210FIELDS.CELL);
            add(ST210FIELDS.LAT);
            add(ST210FIELDS.LON);
            add(ST210FIELDS.SPD);
            add(ST210FIELDS.CRS);
            add(ST210FIELDS.SATT);
            add(ST210FIELDS.FIX);
            add(ST210FIELDS.DIST);
            add(ST210FIELDS.PWR_VOLT);
            add(ST210FIELDS.IO);
            add(ST210FIELDS.EMG_ID);
        }

    };

    @SuppressWarnings("serial")
    static private List<ST210FIELDS> EventProtocol = new LinkedList<ST210FIELDS>() {

        {
            add(ST210FIELDS.HDR_EVENT);
            add(ST210FIELDS.DEV_ID);
            add(ST210FIELDS.SW_VER);
            add(ST210FIELDS.DATE);
            add(ST210FIELDS.TIME);
            add(ST210FIELDS.CELL);
            add(ST210FIELDS.LAT);
            add(ST210FIELDS.LON);
            add(ST210FIELDS.SPD);
            add(ST210FIELDS.CRS);
            add(ST210FIELDS.SATT);
            add(ST210FIELDS.FIX);
            add(ST210FIELDS.DIST);
            add(ST210FIELDS.PWR_VOLT);
            add(ST210FIELDS.IO);
            add(ST210FIELDS.EVT_ID);
        }

    };

    @SuppressWarnings("serial")
    static private List<ST210FIELDS> AlertProtocol = new LinkedList<ST210FIELDS>() {

        {
            add(ST210FIELDS.HDR_ALERT);
            add(ST210FIELDS.DEV_ID);
            add(ST210FIELDS.SW_VER);
            add(ST210FIELDS.DATE);
            add(ST210FIELDS.TIME);
            add(ST210FIELDS.CELL);
            add(ST210FIELDS.LAT);
            add(ST210FIELDS.LON);
            add(ST210FIELDS.SPD);
            add(ST210FIELDS.CRS);
            add(ST210FIELDS.SATT);
            add(ST210FIELDS.FIX);
            add(ST210FIELDS.DIST);
            add(ST210FIELDS.PWR_VOLT);
            add(ST210FIELDS.IO);
            add(ST210FIELDS.ALERT_ID);
        }

    };

    @SuppressWarnings("serial")
    static private List<ST210FIELDS> AliveProtocol = new LinkedList<ST210FIELDS>() {

        {
            add(ST210FIELDS.HDR_ALIVE);
            add(ST210FIELDS.DEV_ID);
        }

    };

    @Override
    public Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) {
        String sentence = (String) msg;
        Log.info("Msg: " + msg);

        Position position = null;

        try{
            position = decodeMsg(sentence);
            Log.info("MESSAGE DECODED WITH SUCCESS!");
        }
        catch(Exception e){
            Log.severe("ERROR WHILE DECODING MESSAGE: " + e.getMessage());
        }

        return position;
    }

    public Position decodeMsg(String msg) throws Exception {

        ST210REPORTS report = getReportType(msg);

        List<ST210FIELDS> protocol = report.getProtocol();

        Pattern protocolPattern = report.getProtocolPattern();

        // Parse message
        Matcher parser = protocolPattern.matcher(msg);
        if (!parser.matches()) {
            throw new Exception("Pattern no match: " + protocolPattern.toString());
        }

        if(report.equals(ST210REPORTS.ALIVE)){
            return null;
        }

        // Create new position
        Position position = new Position();

        position.setAltitude(0D);
        position.setExtendedInfo(new ExtendedInfoFormatter("st210").toString());
        position.setValid(true);

        Integer index = 0;
        for (ST210FIELDS field : protocol) {

            String groupValue = parser.group(index++);

            field.populatePosition(position, groupValue, getDataManager());
        }

        return position;
    }

}
