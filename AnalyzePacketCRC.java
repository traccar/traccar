public class AnalyzePacketCRC {
    
    public static void main(String[] args) {
        // Full packet
        String hex = "7e7e7e7e00720324080711e008000c691d44080a100a3bde4209fe050009276e0000000007a46b032000700038d2647e7e";
        
        System.out.println("=== Packet Analysis ===");
        System.out.println("Full hex: " + hex);
        System.out.println("Length: " + hex.length()/2 + " bytes");
        
        // Remove preamble: 7e7e7e7e00
        String payload = hex.substring(10);
        System.out.println("\nPayload (without preamble): " + payload);
        System.out.println("Payload length: " + payload.length()/2 + " bytes");
        
        // Remove suffix: 7e7e
        String payloadNoSuffix = payload.substring(0, payload.length() - 4);
        System.out.println("\nPayload (without suffix): " + payloadNoSuffix);
        System.out.println("Length: " + payloadNoSuffix.length()/2 + " bytes");
        
        byte[] data = hexStringToByteArray(payloadNoSuffix);
        
        // Parse header
        int type = data[0] & 0xFF;
        int seq = data[1] & 0xFF;
        int size = data[2] & 0xFF;
        int boxIdHigh = data[3] & 0xFF;
        int boxIdLow = data[4] & 0xFF;
        int boxId = (boxIdHigh << 8) | boxIdLow;
        
        System.out.println("\n=== Header ===");
        System.out.println("Type: 0x" + String.format("%02X", type) + " (" + type + ")");
        System.out.println("Sequence: " + seq);
        System.out.println("Size: " + size + " bytes");
        System.out.println("BoxID: 0x" + String.format("%04X", boxId) + " (" + boxId + ")");
        System.out.println("Device ID: " + (1300000 + boxId));
        
        // CRC should be last 2 bytes
        int actualLength = data.length;
        int crcPos = actualLength - 2;
        int crcHigh = data[crcPos] & 0xFF;
        int crcLow = data[crcPos + 1] & 0xFF;
        int receivedCrc = (crcHigh << 8) | crcLow;
        
        System.out.println("\n=== CRC Position ===");
        System.out.println("Actual payload length: " + actualLength + " bytes");
        System.out.println("CRC at position [" + crcPos + "-" + (crcPos+1) + "]: 0x" + 
            String.format("%04X", receivedCrc) + " (" + receivedCrc + ")");
        
        // Data for CRC calculation (Type to before CRC)
        int dataLength = crcPos;
        System.out.println("Data length for CRC: " + dataLength + " bytes");
        System.out.println("Size field says: " + size + " bytes");
        
        if (dataLength + 2 != size) {
            System.out.println("\n⚠️  WARNING: Size mismatch!");
            System.out.println("   Size field: " + size);
            System.out.println("   Actual (data + CRC): " + (dataLength + 2));
            System.out.println("   Difference: " + (size - (dataLength + 2)));
        }
        
        // Calculate CRC
        int calculatedCrc = calculateCrc16(data, 0, dataLength);
        
        System.out.println("\n=== CRC Validation ===");
        System.out.println("Received CRC:   0x" + String.format("%04X", receivedCrc));
        System.out.println("Calculated CRC: 0x" + String.format("%04X", calculatedCrc));
        System.out.println("Match: " + (receivedCrc == calculatedCrc ? "✅ YES" : "❌ NO"));
        
        // Try different CRC positions
        System.out.println("\n=== Try Different Interpretations ===");
        
        // Maybe the last 4 bytes before suffix contain something else?
        System.out.println("\nLast 8 bytes of payload:");
        for (int i = Math.max(0, actualLength - 8); i < actualLength; i++) {
            System.out.printf("  [%d] = 0x%02X (%d)%s\n", 
                i, data[i] & 0xFF, data[i] & 0xFF,
                i == crcPos || i == crcPos+1 ? " <- CRC" : "");
        }
        
        // What if CRC is at different position?
        System.out.println("\nIf we use Size field (" + size + " bytes):");
        if (size <= actualLength) {
            int altCrcPos = size - 2;
            int altCrcHigh = data[altCrcPos] & 0xFF;
            int altCrcLow = data[altCrcPos + 1] & 0xFF;
            int altReceivedCrc = (altCrcHigh << 8) | altCrcLow;
            int altCalculatedCrc = calculateCrc16(data, 0, altCrcPos);
            
            System.out.println("  CRC at position [" + altCrcPos + "-" + (altCrcPos+1) + "]: 0x" + 
                String.format("%04X", altReceivedCrc));
            System.out.println("  Calculated: 0x" + String.format("%04X", altCalculatedCrc));
            System.out.println("  Match: " + (altReceivedCrc == altCalculatedCrc ? "✅ YES" : "❌ NO"));
        }
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
