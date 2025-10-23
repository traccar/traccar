public class TestDecodePacket {
    
    public static void main(String[] args) {
        String fullHex = "7E7E7E7E006D5384189811E1061D335723B1099171EE3D0E9AD7FF010010291103B0000007E3000000880000000000140208000500010A8EB0C600000000000000002C00012C0000000000000000000000000000000000000000000000001600000000000000000000000000000000000000000000000000000000000000000000000000000000005555EDBC7E7E";
        
        System.out.println("=== Full Packet Analysis ===");
        System.out.println("Full hex: " + fullHex);
        System.out.println("Length: " + fullHex.length()/2 + " bytes");
        
        // Remove preamble (5 bytes = 10 hex chars)
        String withoutPreamble = fullHex.substring(10);
        System.out.println("\nWithout preamble: " + withoutPreamble);
        System.out.println("Length: " + withoutPreamble.length()/2 + " bytes");
        
        // Remove suffix (4 bytes = 8 hex chars from end)
        String payload = withoutPreamble.substring(0, withoutPreamble.length() - 8);
        System.out.println("\nPayload: " + payload);
        System.out.println("Length: " + payload.length()/2 + " bytes");
        
        byte[] data = hexStringToByteArray(payload);
        
        // Parse header
        int type = data[0] & 0xFF;
        int seq = data[1] & 0xFF;
        int size = data[2] & 0xFF;
        int boxId = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
        
        System.out.println("\n=== Header ===");
        System.out.println("Type: 0x" + String.format("%02X", type) + " (" + type + ")");
        System.out.println("Seq: " + seq);
        System.out.println("Size: " + size + " bytes");
        System.out.println("BoxID: 0x" + String.format("%04X", boxId) + " (" + boxId + ")");
        System.out.println("Device: " + (1300000 + boxId));
        
        // CRC validation
        System.out.println("\n=== CRC Validation ===");
        
        // Try last 2 bytes
        int pos1 = data.length - 2;
        int crc1 = ((data[pos1] & 0xFF) << 8) | (data[pos1 + 1] & 0xFF);
        int calc1 = calculateCrc16(data, 0, pos1);
        System.out.println("Last 2 bytes [" + pos1 + "-" + (pos1+1) + "]:");
        System.out.println("  Received: 0x" + String.format("%04X", crc1));
        System.out.println("  Calculated: 0x" + String.format("%04X", calc1));
        System.out.println("  Match: " + (crc1 == calc1 ? "✅" : "❌"));
        
        // Try size - 2
        int pos2 = size - 2;
        if (pos2 > 0 && pos2 < data.length - 1) {
            int crc2 = ((data[pos2] & 0xFF) << 8) | (data[pos2 + 1] & 0xFF);
            int calc2 = calculateCrc16(data, 0, pos2);
            System.out.println("\nAt Size-2 [" + pos2 + "-" + (pos2+1) + "]:");
            System.out.println("  Received: 0x" + String.format("%04X", crc2));
            System.out.println("  Calculated: 0x" + String.format("%04X", calc2));
            System.out.println("  Match: " + (crc2 == calc2 ? "✅" : "❌"));
        }
        
        // GPS32 record
        System.out.println("\n=== GPS32 Record (32 bytes from position 5) ===");
        if (data.length >= 37) {
            for (int i = 5; i < 37; i++) {
                if ((i - 5) % 16 == 0) System.out.printf("\n[%02d]: ", i);
                System.out.printf("%02X ", data[i] & 0xFF);
            }
            System.out.println();
            
            // Parse GPS32
            int recordType = data[5] & 0xFF;
            int flagDegree = data[6] & 0xFF;
            int hdop = data[7] & 0xFF;
            int speed = data[8] & 0xFF;
            
            System.out.println("\nGPS32 Details:");
            System.out.println("  RecordType: 0x" + String.format("%02X", recordType));
            System.out.println("  FlagDegree: 0x" + String.format("%02X", flagDegree));
            System.out.println("  HDOP: " + hdop);
            System.out.println("  Speed: " + speed + " (raw)");
            
            // Parse flags
            boolean east = (flagDegree & 0x80) != 0;
            boolean north = (flagDegree & 0x40) != 0;
            boolean valid = (flagDegree & 0x20) != 0;
            int course = (flagDegree & 0x1F) * 360 / 32;
            
            System.out.println("  East: " + east);
            System.out.println("  North: " + north);
            System.out.println("  Valid: " + valid);
            System.out.println("  Course: " + course + "°");
        }
        
        // Check message type
        System.out.println("\n=== Message Type ===");
        if (type == 109) {
            System.out.println("Type 109 = M_PING_REPLY_ENHIO");
            System.out.println("This is a ping reply with extended I/O data");
        } else if (type == 114) {
            System.out.println("Type 114 = M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO");
        } else if (type == 115) {
            System.out.println("Type 115 = M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO");
        } else {
            System.out.println("Type " + type + " = Unknown or other message type");
        }
        
        System.out.println("\n=== Suffix (from full packet) ===");
        String suffix = fullHex.substring(fullHex.length() - 8);
        System.out.println("Suffix: " + suffix);
        System.out.println("Expected: 7E7E (but decoder ignores it)");
    }
    
    private static int calculateCrc16(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            crc ^= (data[i] & 0xFF) << 8;
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc = crc << 1;
                }
            }
        }
        return crc & 0xFFFF;
    }
    
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
