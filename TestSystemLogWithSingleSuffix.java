import java.nio.charset.StandardCharsets;

public class TestSystemLogWithSingleSuffix {
    
    public static void main(String[] args) {
        // System Log packet: 7E7E7E7E00 61 05 C9 1A 0B ... 86 7E
        String hex = "7e7e7e7e00" + 
                     "6105c91a0b" +
                     "2358" +                           // CRC (should be here)
                     "2c31393133" + "30363637352c" +   // ASCII: ,191306675,
                     "3032302c30" + "30303735";        // ASCII: 020,00075
        
        // Parse header
        byte[] data = hexStringToByteArray(hex.substring(10)); // Skip preamble
        
        int type = data[0] & 0xFF;      // 0x61 = 97
        int seq = data[1] & 0xFF;       // 0x05 = 5
        int size = data[2] & 0xFF;      // 0xC9 = 201
        int boxIdHigh = data[3] & 0xFF; // 0x1A = 26
        int boxIdLow = data[4] & 0xFF;  // 0x0B = 11
        int boxId = (boxIdHigh << 8) | boxIdLow; // 0x1A0B = 6675
        
        System.out.println("=== System Log Packet Analysis ===");
        System.out.println("Type: 0x" + Integer.toHexString(type) + " (" + type + ")");
        System.out.println("Sequence: " + seq);
        System.out.println("Size: " + size + " bytes");
        System.out.println("BoxID: " + boxId + " (Device: " + (1300000 + boxId) + ")");
        
        // Expected: payload after header
        int crcPos1 = data[5] & 0xFF;
        int crcPos2 = data[6] & 0xFF;
        int crcValue = (crcPos1 << 8) | crcPos2;
        System.out.println("\nBytes at position 5-6: 0x" + 
            String.format("%02X%02X", crcPos1, crcPos2) + 
            " (" + crcValue + ")");
        
        // Check if it's ASCII
        String ascii = new String(data, 5, Math.min(20, data.length - 5), StandardCharsets.US_ASCII);
        System.out.println("ASCII interpretation: " + ascii);
        
        System.out.println("\n=== Frame Decoder Expectation ===");
        System.out.println("Preamble: 7E7E7E7E00");
        System.out.println("Payload: Type|Seq|Size|BoxID|Data|CRC");
        System.out.println("Suffix: 7E7E or 0000 or single 7E");
        System.out.println("\nIssue: This packet ends with single 7E");
        System.out.println("Solution: Modified frame decoder to accept single 7E as fallback");
    }
    
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
