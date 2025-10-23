public class TestUserPacket {
    
    public static void main(String[] args) {
        // User's packet: 7e7e7e7e00 + payload + 7e7e
        String fullHex = "7e7e7e7e00720324080711e008000c691d44080a100a3bde4209fe050009276e0000000007a46b032000700038d2647e7e";
        
        // After frame decoder removes preamble and suffix:
        String payload = fullHex.substring(10, fullHex.length() - 4);
        
        System.out.println("=== User's Packet ===");
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
        
        // CRC at LAST 2 bytes
        int dataLen = data.length - 2;
        int crcHigh = data[dataLen] & 0xFF;
        int crcLow = data[dataLen + 1] & 0xFF;
        int receivedCrc = (crcHigh << 8) | crcLow;
        
        int calculatedCrc = calculateCrc16(data, 0, dataLen);
        
        System.out.println("\n=== CRC Validation (Last 2 Bytes) ===");
        System.out.println("Data length: " + dataLen + " bytes");
        System.out.println("Received CRC: 0x" + String.format("%04X", receivedCrc) + " (at position " + dataLen + ")");
        System.out.println("Calculated CRC: 0x" + String.format("%04X", calculatedCrc));
        System.out.println("Match: " + (receivedCrc == calculatedCrc ? "✅ YES" : "❌ NO"));
        
        // Show difference between size field and actual length
        System.out.println("\n=== Trailing Bytes ===");
        System.out.println("Size field: " + size + " bytes");
        System.out.println("Actual length: " + data.length + " bytes");
        System.out.println("Trailing: " + (data.length - size) + " bytes");
        
        if (data.length > size) {
            System.out.print("Trailing bytes: ");
            for (int i = size; i < data.length; i++) {
                System.out.printf("%02X ", data[i] & 0xFF);
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
