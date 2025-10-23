import java.util.Calendar;
import java.util.TimeZone;

public class TestPingReply {
    public static void main(String[] args) {
        // Ping reply packet
        String hex = "6D458405F911E207000C6904F6046710983B9E23E8FE01000B273778200067500063C000720000000000000208000300000150992C00000000223347222C00002C07380000000000000000000000000000000000000000000000B40000000000000000000000000000000000000000000000000000000000000000000000000000000055559EEE";
        
        System.out.println("=== GTR-9 Ping Reply EnhIO (Type 109) ===\n");
        
        // Parse header
        int type = 0x6D; // 109
        int seq = 0x45; // 69
        int size = 0x84; // 132
        int boxId = 0x05F9; // 1529
        
        System.out.println("Header:");
        System.out.println("  Type: 0x" + String.format("%02X", type) + " (" + type + ") - PING_REPLY_ENHIO");
        System.out.println("  Sequence: " + seq);
        System.out.println("  Size: " + size + " bytes");
        System.out.println("  BoxID: " + boxId + " → Device " + (1300000 + boxId));
        
        // Parse GPS32 record (32 bytes)
        int recordType = 0x11;
        int flagDegree = 0xE2;
        int hdop = 0x07;
        int speedRaw = 0x00;
        long datetimeRaw = 0x0C6904F6L;
        long latRaw = 0x04671098L;
        long lonRaw = 0x3B9E23E8L;
        int digi16 = 0xFE01;
        int opt16 = 0x000B;
        int altRaw = 0x2737;
        int ana01 = 0x782000;
        int ana23 = 0x675000;
        int ana45 = 0x63C000;
        int recordCrc = 0x72;
        
        System.out.println("\n=== GPS32 Record (32 bytes) ===");
        System.out.println("Record Type: " + recordType);
        
        // Parse flags
        boolean east = (flagDegree & 0x80) != 0;
        boolean north = (flagDegree & 0x40) != 0;
        boolean gpsValid = (flagDegree & 0x20) != 0;
        int courseBits = flagDegree & 0x1F;
        double course = courseBits * 360.0 / 32.0;
        
        System.out.println("Flag/Degree: 0x" + String.format("%02X", flagDegree));
        System.out.println("  East: " + east + ", North: " + north + ", Valid: " + gpsValid);
        System.out.println("  Course: " + course + "°");
        System.out.println("HDOP: " + hdop);
        System.out.println("Speed: " + speedRaw + " → " + (speedRaw * 1.852 * 0.539957) + " knots");
        
        // Parse coordinates
        long latDegrees = latRaw / 10000000L;
        long latMinutes = latRaw % 10000000L;
        double latitude = latDegrees + (latMinutes / 6000000.0);
        
        long lonDegrees = lonRaw / 10000000L;
        long lonMinutes = lonRaw % 10000000L;
        double longitude = lonDegrees + (lonMinutes / 6000000.0);
        
        if (!north) latitude = -latitude;
        if (!east) longitude = -longitude;
        
        System.out.println("Latitude: " + latitude + "°");
        System.out.println("Longitude: " + longitude + "°");
        System.out.println("Satellites: " + (opt16 & 0xFF));
        
        double altitude = (altRaw - 10000) * 0.3048;
        System.out.println("Altitude: " + altitude + " m");
        
        // Analog values
        int ana0 = (ana01 >> 12) & 0xFFF;
        int ana1 = ana01 & 0xFFF;
        int ana2 = (ana23 >> 12) & 0xFFF;
        int ana3 = ana23 & 0xFFF;
        int ana4 = (ana45 >> 12) & 0xFFF;
        int ana5 = ana45 & 0xFFF;
        
        System.out.println("Analog: ana0=" + ana0 + ", ana1=" + ana1 + ", ana2=" + ana2 + 
                          ", ana3=" + ana3 + ", ana4=" + ana4 + ", ana5=" + ana5);
        System.out.println("Record CRC: 0x" + String.format("%02X", recordCrc));
        
        // Decode datetime
        int seconds = (int) ((datetimeRaw & 0x1F) * 2);
        int minutes = (int) ((datetimeRaw >> 5) & 0x3F);
        int hours = (int) ((datetimeRaw >> 11) & 0x1F);
        int day = (int) ((datetimeRaw >> 16) & 0x1F);
        int month = (int) ((datetimeRaw >> 21) & 0x0F);
        int year = (int) ((datetimeRaw >> 25) & 0x7F) + 2000;
        
        System.out.println("\nDateTime: 0x" + String.format("%08X", datetimeRaw));
        System.out.println("  Raw: " + year + "-" + month + "-" + day + " " + hours + ":" + minutes + ":" + seconds);
        
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month - 1, day, hours, minutes, seconds);
        
        // Apply GPS rollover if needed
        if (year < 2008) {
            long original = calendar.getTimeInMillis();
            long adjusted = original + 619_315_200_000L;
            long now = System.currentTimeMillis();
            
            if (Math.abs(now - adjusted) < Math.abs(now - original)) {
                calendar.setTimeInMillis(adjusted);
                System.out.println("  GPS rollover applied!");
            }
        }
        
        System.out.println("  Final: " + calendar.getTime());
        
        // Parse ping reply data (96 bytes)
        System.out.println("\n=== Ping Reply Data (96 bytes) ===");
        int offlinePtr = 0x0000;
        int idSec = 0x0000;
        int sTime = 0x0000;
        int mcc = 0x0208;
        int mnc = 0x0003;
        int ltCell = 0x0000;
        long cellId = 0x0150992CL;
        int ta = 0x00;
        int tc = 0x00;
        int ltbs = 0x0000;
        String baseStation = "3G";
        int rssi = 0x00;
        int ltc13 = 0xB400;
        int sync = 0x5555;
        
        System.out.println("Offline Pointer: " + offlinePtr);
        System.out.println("ID Sec: " + idSec);
        System.out.println("S Time: " + sTime);
        System.out.println("MCC: " + mcc + " (Thailand)");
        System.out.println("MNC: " + mnc);
        System.out.println("LT Cell: " + ltCell);
        System.out.println("Cell ID: 0x" + String.format("%08X", cellId));
        System.out.println("Timing Advance: " + ta);
        System.out.println("Timing Correction: " + tc);
        System.out.println("Base Station Timing: " + ltbs);
        System.out.println("Base Station: " + baseStation);
        System.out.println("RSSI: " + rssi);
        System.out.println("Neighbor Timing: " + ltc13);
        System.out.println("Sync: 0x" + String.format("%04X", sync) + " (" + sync + ")");
        
        System.out.println("\n=== Expected Values ===");
        System.out.println("DateTime: 2025-10-23 07:39:44");
        System.out.println("Latitude: 7.643886666666667°");
        System.out.println("Longitude: 100.036604°");
        System.out.println("Satellites: 11");
        System.out.println("Altitude: 11.8872 m");
        System.out.println("ana0=1922, ana1=0, ana2=1653, ana3=0, ana4=1596, ana5=0");
        System.out.println("Cell ID: 0x0150992C");
        System.out.println("MCC: 520 (0x0208), MNC: 3");
    }
}
