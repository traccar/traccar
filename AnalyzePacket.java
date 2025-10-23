public class AnalyzePacket {
    public static void main(String[] args) {
        // Raw packet: 7e7e7e7e00 73092410ab 11e005003357110303c278c63bc19e82fe050012282d00000000079800000000700028 746b 0000
        String hex = "7e7e7e7e0073092410ab11e005003357110303c278c63bc19e82fe050012282d00000000079800000000700028746b0000";
        
        System.out.println("=== Packet Analysis ===");
        System.out.println("Full packet: " + hex);
        System.out.println("Length: " + hex.length() / 2 + " bytes");
        
        // After frame decoder removes preamble (7e7e7e7e00) and trailer (7e7e)
        // Expected: 73 09 24 10ab ...data... CRC16
        String afterFraming = "73092410ab11e005003357110303c278c63bc19e82fe050012282d00000000079800000000700028746b0000";
        
        System.out.println("\nAfter framing removal: " + afterFraming);
        System.out.println("Length: " + afterFraming.length() / 2 + " bytes");
        
        // Parse header
        int type = Integer.parseInt(afterFraming.substring(0, 2), 16);
        int seq = Integer.parseInt(afterFraming.substring(2, 4), 16);
        int size = Integer.parseInt(afterFraming.substring(4, 6), 16);
        int boxIdH = Integer.parseInt(afterFraming.substring(6, 8), 16);
        int boxIdL = Integer.parseInt(afterFraming.substring(8, 10), 16);
        int boxId = (boxIdH << 8) | boxIdL;
        
        System.out.println("\n=== Header ===");
        System.out.println("Type: 0x" + String.format("%02X", type) + " (" + type + ")");
        System.out.println("Sequence: 0x" + String.format("%02X", seq) + " (" + seq + ")");
        System.out.println("Size: 0x" + String.format("%02X", size) + " (" + size + " bytes)");
        System.out.println("BoxID: 0x" + String.format("%04X", boxId) + " (" + boxId + ")");
        System.out.println("Device ID: " + (1300000 + boxId));
        
        // Data starts at byte 5 (after Type, Seq, Size, BoxID)
        String data = afterFraming.substring(10);
        System.out.println("\n=== Data + CRC ===");
        System.out.println("Data length: " + data.length() / 2 + " bytes");
        System.out.println("Data: " + data);
        
        // Last 2 bytes should be CRC
        if (data.length() >= 4) {
            String dataOnly = data.substring(0, data.length() - 4);
            String crc = data.substring(data.length() - 4);
            
            System.out.println("\nData (without CRC): " + dataOnly);
            System.out.println("Data length: " + dataOnly.length() / 2 + " bytes");
            System.out.println("CRC in packet: 0x" + crc);
        }
        
        // Check if this matches size field
        int totalBytes = afterFraming.length() / 2;
        int headerBytes = 5; // Type(1) + Seq(1) + Size(1) + BoxID(2)
        int dataBytes = totalBytes - headerBytes;
        
        System.out.println("\n=== Size Analysis ===");
        System.out.println("Total bytes: " + totalBytes);
        System.out.println("Header bytes: " + headerBytes);
        System.out.println("Data+CRC bytes: " + dataBytes);
        System.out.println("Size field says: " + size + " bytes");
        System.out.println("Match: " + (dataBytes == size ? "YES" : "NO - MISMATCH!"));
        
        // Analyze message type
        System.out.println("\n=== Message Type Analysis ===");
        System.out.println("Type 0x73 (115) = M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO");
        System.out.println("But decoder reads Type as 0x" + String.format("%02X", type));
        
        if (type == 0x73) {
            System.out.println("✓ Correct - offline position report");
        } else if (type == 0x7E) {
            System.out.println("⚠ ERROR: Type is 0x7E (frame delimiter!)");
            System.out.println("This means the frame decoder didn't remove preamble correctly!");
        }
        
        // Try parsing from different position
        System.out.println("\n=== Alternative Parsing (if 7E not removed) ===");
        String alternative = "73092410ab11e005003357110303c278c63bc19e82fe050012282d00000000079800000000700028746b";
        type = Integer.parseInt(alternative.substring(0, 2), 16);
        seq = Integer.parseInt(alternative.substring(2, 4), 16);
        size = Integer.parseInt(alternative.substring(4, 6), 16);
        boxIdH = Integer.parseInt(alternative.substring(6, 8), 16);
        boxIdL = Integer.parseInt(alternative.substring(8, 10), 16);
        boxId = (boxIdH << 8) | boxIdL;
        
        System.out.println("Type: 0x" + String.format("%02X", type) + " (" + type + ")");
        System.out.println("Sequence: " + seq);
        System.out.println("Size: " + size + " bytes");
        System.out.println("BoxID: 0x" + String.format("%04X", boxId) + " (" + boxId + ")");
        System.out.println("Device ID: " + (1300000 + boxId));
        
        // Calculate expected CRC location
        int dataLen = size - 2; // Size includes CRC
        String dataForCrc = alternative.substring(0, 10 + dataLen * 2);
        String expectedCrc = alternative.substring(10 + dataLen * 2, 10 + dataLen * 2 + 4);
        
        System.out.println("\nData for CRC: " + dataForCrc);
        System.out.println("Expected CRC position: " + expectedCrc);
    }
}
