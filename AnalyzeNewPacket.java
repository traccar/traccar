public class AnalyzeNewPacket {
    
    public static void main(String[] args) {
        // Full packet from log
        String fullHex = "7e7e7e7e00720924199411e0060033571f79086117f23bd381b0ff05001127210dd000000824000fd9007000a8b52f7e7e";
        
        // After frame decoder removes preamble and suffix
        String payload = fullHex.substring(10, fullHex.length() - 4);
        
        System.out.println("=== Packet Analysis ===");
        System.out.println("Full hex: " + fullHex);
        System.out.println("\nPayload: " + payload);
        
        byte[] data = hexStringToByteArray(payload);
        System.out.println("Payload length: " + data.length + " bytes");
        
        // Parse header
        int type = data[0] & 0xFF;
        int seq = data[1] & 0xFF;
        int size = data[2] & 0xFF;
        int boxIdHigh = data[3] & 0xFF;
        int boxIdLow = data[4] & 0xFF;
        int boxId = (boxIdHigh << 8) | boxIdLow;
        
        System.out.println("\n=== Header ===");
        System.out.println("Type: 0x" + String.format("%02X", type) + " (" + type + ")");
        System.out.println("Seq: " + seq);
        System.out.println("Size field: " + size + " bytes");
        System.out.println("BoxID: 0x" + String.format("%04X", boxId) + " (" + boxId + ")");
        System.out.println("Device: " + (1300000 + boxId));
        
        System.out.println("\n=== Check Different CRC Positions ===");
        
        // 1. CRC at last 2 bytes (current logic)
        int dataLen1 = data.length - 2;
        int crc1 = ((data[dataLen1] & 0xFF) << 8) | (data[dataLen1 + 1] & 0xFF);
        int calc1 = calculateCrc16(data, 0, dataLen1);
        System.out.println("1. Last 2 bytes (position " + dataLen1 + "):");
        System.out.println("   Received: 0x" + String.format("%04X", crc1));
        System.out.println("   Calculated: 0x" + String.format("%04X", calc1));
        System.out.println("   Match: " + (crc1 == calc1 ? "✅" : "❌"));
        
        // 2. CRC at size field position
        int dataLen2 = size - 2;
        if (dataLen2 > 0 && dataLen2 < data.length - 1) {
            int crc2 = ((data[dataLen2] & 0xFF) << 8) | (data[dataLen2 + 1] & 0xFF);
            int calc2 = calculateCrc16(data, 0, dataLen2);
            System.out.println("\n2. At Size field position " + dataLen2 + " (Size=" + size + " - 2):");
            System.out.println("   Received: 0x" + String.format("%04X", crc2));
            System.out.println("   Calculated: 0x" + String.format("%04X", calc2));
            System.out.println("   Match: " + (crc2 == calc2 ? "✅" : "❌"));
        }
        
        // 3. What the log shows
        System.out.println("\n3. From log:");
        System.out.println("   Received: 0x0DD0");
        System.out.println("   Calculated: 0xEFDC");
        System.out.println("   dataLength: 27");
        
        // Find where 0DD0 appears
        System.out.println("\n=== Search for 0DD0 in packet ===");
        for (int i = 0; i < data.length - 1; i++) {
            int word = ((data[i] & 0xFF) << 8) | (data[i + 1] & 0xFF);
            if (word == 0x0DD0) {
                System.out.println("Found 0x0DD0 at position [" + i + "-" + (i+1) + "]");
                System.out.println("This is " + (data.length - i - 2) + " bytes from the end");
                
                // Try CRC with this position
                int calcCrc = calculateCrc16(data, 0, i);
                System.out.println("If CRC is here, calculated would be: 0x" + String.format("%04X", calcCrc));
            }
        }
        
        // Show all bytes
        System.out.println("\n=== Full Payload Breakdown ===");
        for (int i = 0; i < data.length; i++) {
            if (i % 16 == 0) {
                System.out.printf("\n[%02d]: ", i);
            }
            System.out.printf("%02X ", data[i] & 0xFF);
        }
        System.out.println();
        
        // GPS32 record starts at position 5
        System.out.println("\n=== GPS32 Record (32 bytes from position 5) ===");
        if (data.length >= 37) {
            int gpsStart = 5;
            System.out.print("GPS32: ");
            for (int i = gpsStart; i < gpsStart + 32 && i < data.length; i++) {
                System.out.printf("%02X ", data[i] & 0xFF);
            }
            System.out.println();
            System.out.println("After GPS32 (position 37+):");
            for (int i = 37; i < data.length; i++) {
                System.out.printf("[%d]=0x%02X ", i, data[i] & 0xFF);
            }
            System.out.println();
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
