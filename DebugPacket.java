public class DebugPacket {
    
    public static void main(String[] args) {
        // First test packet
        String hex = "723C2418FC11EA07003356B2400467187C3B9E21D2FE01000D27AE0000000007A800BD000000B640B0EC";
        byte[] data = hexStringToByteArray(hex);
        
        System.out.println("=== Packet Analysis ===");
        System.out.println("Hex: " + hex);
        System.out.println("Length: " + data.length + " bytes");
        
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
        System.out.println("Actual length: " + data.length + " bytes");
        System.out.println("Difference: " + (data.length - size) + " bytes");
        
        // Calculate CRC for size bytes
        int dataLenForCrc = size - 2;
        System.out.println("\n=== CRC Calculation ===");
        System.out.println("Data length for CRC: " + dataLenForCrc + " bytes");
        System.out.println("CRC position: [" + dataLenForCrc + "-" + (dataLenForCrc+1) + "]");
        
        if (dataLenForCrc + 2 <= data.length) {
            int receivedCrc = ((data[dataLenForCrc] & 0xFF) << 8) | (data[dataLenForCrc + 1] & 0xFF);
            int calculatedCrc = calculateCrc16(data, 0, dataLenForCrc);
            
            System.out.println("Received CRC: 0x" + String.format("%04X", receivedCrc));
            System.out.println("Calculated CRC: 0x" + String.format("%04X", calculatedCrc));
            System.out.println("Match: " + (receivedCrc == calculatedCrc ? "✅ YES" : "❌ NO"));
            
            // Show trailing bytes
            if (data.length > size) {
                System.out.println("\n=== Trailing Bytes ===");
                System.out.print("Bytes after CRC: ");
                for (int i = size; i < data.length; i++) {
                    System.out.printf("%02X ", data[i] & 0xFF);
                }
                System.out.println("(" + (data.length - size) + " bytes)");
            }
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
