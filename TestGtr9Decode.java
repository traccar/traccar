import java.util.Calendar;
import java.util.TimeZone;

public class TestGtr9Decode {
    public static void main(String[] args) {
        // Packet data: 11E207000C690D510467110F3B9E23B7FE010009273778300067300063BD000000B605
        
        // Parse GPS32 record (32 bytes)
        int recordType = 0x11;
        int flagDegree = 0xE2;
        int hdop = 0x07;
        int speedRaw = 0x00;
        long datetimeRaw = 0x0C690D51L;
        long latRaw = 0x0467110FL;
        long lonRaw = 0x3B9E23B7L;
        int digi16 = 0xFE01;
        int opt16 = 0x0009;
        int altRaw = 0x2737;
        int ana01 = 0x783000;
        int ana23 = 0x673000;
        int ana45 = 0x63BD00;
        int recordCrc = 0x05;
        
        System.out.println("=== GPS32 Record Decode ===");
        System.out.println("Record Type: " + recordType);
        System.out.println("Flag/Degree: 0x" + String.format("%02X", flagDegree));
        
        // Parse flags
        boolean east = (flagDegree & 0x80) != 0;
        boolean north = (flagDegree & 0x40) != 0;
        boolean gpsValid = (flagDegree & 0x20) != 0;
        int courseBits = flagDegree & 0x1F;
        double course = courseBits * 360.0 / 32.0;
        
        System.out.println("  East: " + east + ", North: " + north + ", Valid: " + gpsValid);
        System.out.println("  Course bits: " + courseBits + " → " + course + "°");
        
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
        
        System.out.println("Latitude: 0x" + String.format("%08X", latRaw) + " → " + latitude + "°");
        System.out.println("Longitude: 0x" + String.format("%08X", lonRaw) + " → " + longitude + "°");
        
        System.out.println("Digital: 0x" + String.format("%04X", digi16));
        System.out.println("Opt16: 0x" + String.format("%04X", opt16) + " → Satellites: " + (opt16 & 0xFF));
        
        // Altitude
        double altitude = (altRaw - 10000) * 0.3048;
        System.out.println("Altitude: 0x" + String.format("%04X", altRaw) + " → " + altitude + " m");
        
        // Analog values
        int ana0 = (ana01 >> 12) & 0xFFF;
        int ana1 = ana01 & 0xFFF;
        int ana2 = (ana23 >> 12) & 0xFFF;
        int ana3 = ana23 & 0xFFF;
        int ana4 = (ana45 >> 12) & 0xFFF;
        int ana5 = ana45 & 0xFFF;
        
        System.out.println("Analog values:");
        System.out.println("  ana0: " + ana0 + ", ana1: " + ana1);
        System.out.println("  ana2: " + ana2 + ", ana3: " + ana3);
        System.out.println("  ana4: " + ana4 + ", ana5: " + ana5);
        
        System.out.println("Record CRC: 0x" + String.format("%02X", recordCrc));
        
        // Decode datetime
        int seconds = (int) ((datetimeRaw & 0x1F) * 2);
        int minutes = (int) ((datetimeRaw >> 5) & 0x3F);
        int hours = (int) ((datetimeRaw >> 11) & 0x1F);
        int day = (int) ((datetimeRaw >> 16) & 0x1F);
        int month = (int) ((datetimeRaw >> 21) & 0x0F);
        int year = (int) ((datetimeRaw >> 25) & 0x7F) + 2000;
        
        System.out.println("\nDateTime: 0x" + String.format("%08X", datetimeRaw));
        System.out.println("  Year: " + year + ", Month: " + month + ", Day: " + day);
        System.out.println("  Hour: " + hours + ", Minute: " + minutes + ", Second: " + seconds);
        
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month - 1, day, hours, minutes, seconds);
        
        System.out.println("  Original timestamp: " + calendar.getTime());
        
        // Apply GPS week rollover correction
        if (year < 2008) {
            long original = calendar.getTimeInMillis();
            long adjusted = original + 619_315_200_000L; // 1024 weeks
            long now = System.currentTimeMillis();
            
            if (Math.abs(now - adjusted) < Math.abs(now - original)) {
                calendar.setTimeInMillis(adjusted);
                System.out.println("  GPS rollover applied!");
                System.out.println("  Corrected timestamp: " + calendar.getTime());
            }
        }
        
        System.out.println("  Final timestamp: " + calendar.getTime());
        
        // Extended data (4 bytes): 000000B605
        System.out.println("\n=== Extended Data ===");
        int event1 = 0x00;
        int event2 = 0x00;
        int event3 = 0x00;
        int extCrc = 0xB6;
        System.out.println("Event1: 0x" + String.format("%02X", event1));
        System.out.println("Event2: 0x" + String.format("%02X", event2));
        System.out.println("Event3: 0x" + String.format("%02X", event3));
        System.out.println("Ext CRC: 0x" + String.format("%02X", extCrc));
        
        System.out.println("\n=== Expected Values (from user) ===");
        System.out.println("DateTime: 2025-10-23 08:42:34");
        System.out.println("Latitude: 7.6439065°");
        System.out.println("Longitude: 100.03659583333334°");
        System.out.println("Course: 22.5° (degree=2)");
        System.out.println("Satellites: 9");
        System.out.println("Altitude: 11.8872 m");
        System.out.println("ana0=1923, ana1=0, ana2=1651, ana3=0, ana4=1595, ana5=3328");
    }
}
