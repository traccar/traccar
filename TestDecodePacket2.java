public class TestDecodePacket2 {
    
    public static void main(String[] args) {
        String fullHex = "7E7E7E7E006D5384189811E1061D335723B1099171EE3D0E9AD7FF010010291103B0000007E3000000880000000000140208000500010A8EB0C600000000000000002C00012C0000000000000000000000000000000000000000000000001600000000000000000000000000000000000000000000000000000000000000000000000000000000005555EDBC7E7E";
        
        System.out.println("=== Test with 2-byte suffix (7E7E) ===");
        
        // Remove preamble (5 bytes = 10 hex chars)
        String withoutPreamble = fullHex.substring(10);
        
        // Remove suffix (2 bytes = 4 hex chars from end)
        String payload = withoutPreamble.substring(0, withoutPreamble.length() - 4);
        
        System.out.println("Payload: " + payload);
        System.out.println("Length: " + payload.length()/2 + " bytes");
        
        byte[] data = hexStringToByteArray(payload);
        
        // Parse header
        int type = data[0] & 0xFF;
        int size = data[2] & 0xFF;
        int boxId = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
        
        System.out.println("\nType: " + type + ", Size: " + size + ", BoxID: " + boxId);
        System.out.println("Device: " + (1300000 + boxId));
        
        // CRC at last 2 bytes
        int pos = data.length - 2;
        int crcRecv = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
        int crcCalc = calculateCrc16(data, 0, pos);
        
        System.out.println("\nCRC at last 2 bytes [" + pos + "-" + (pos+1) + "]:");
        System.out.println("  Received: 0x" + String.format("%04X", crcRecv));
        System.out.println("  Calculated: 0x" + String.format("%04X", crcCalc));
        System.out.println("  Match: " + (crcRecv == crcCalc ? "✅" : "❌"));
        
        System.out.println("\n=== Conclusion ===");
        if (crcRecv == crcCalc) {
            System.out.println("✅ This packet uses 2-byte suffix (7E7E), not 4 bytes!");
            System.out.println("Frame decoder should check suffix length dynamically");
        } else {
            System.out.println("❌ Still not matching. Need to investigate further.");
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
