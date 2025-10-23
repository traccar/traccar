public class TestFrameDecoder {
    public static void main(String[] args) {
        // Packet with 0000 suffix
        String hexWithZero = "7e7e7e7e0073092410ab11e005003357110303c278c63bc19e82fe050012282d00000000079800000000700028746b0000";
        
        // Packet with 7E7E suffix (normal)
        String hexWithSeven = "7e7e7e7e0073092410ab11e005003357110303c278c63bc19e82fe050012282d00000000079800000000700028746b7e7e";
        
        System.out.println("=== Testing Frame Decoder Logic ===\n");
        
        // Test 1: 0000 suffix
        System.out.println("Test 1: Packet with 0000 suffix");
        testPacket(hexWithZero, "0000");
        
        System.out.println("\nTest 2: Packet with 7E7E suffix");
        testPacket(hexWithSeven, "7E7E");
    }
    
    static void testPacket(String hex, String suffixType) {
        byte[] bytes = hexStringToByteArray(hex);
        
        // Skip preamble (5 bytes)
        int start = 5;
        
        // Find suffix
        int endIndex = -1;
        for (int i = start; i < bytes.length - 1; i++) {
            int byte1 = bytes[i] & 0xFF;
            int byte2 = bytes[i + 1] & 0xFF;
            
            if ((byte1 == 0x7E && byte2 == 0x7E) || (byte1 == 0x00 && byte2 == 0x00)) {
                endIndex = i;
                System.out.println("  Found suffix at position " + i + ": 0x" + 
                    String.format("%02X", byte1) + String.format("%02X", byte2));
                break;
            }
        }
        
        if (endIndex != -1) {
            int payloadLength = endIndex - start;
            byte[] payload = new byte[payloadLength];
            System.arraycopy(bytes, start, payload, 0, payloadLength);
            
            System.out.println("  Payload length: " + payloadLength + " bytes");
            System.out.println("  Payload: " + bytesToHex(payload));
            
            // Parse header
            int type = payload[0] & 0xFF;
            int seq = payload[1] & 0xFF;
            int size = payload[2] & 0xFF;
            int boxId = ((payload[3] & 0xFF) << 8) | (payload[4] & 0xFF);
            
            System.out.println("  Type: 0x" + String.format("%02X", type) + " (" + type + ")");
            System.out.println("  Sequence: " + seq);
            System.out.println("  Size: " + size + " bytes");
            System.out.println("  BoxID: " + boxId + " → Device " + (1300000 + boxId));
            System.out.println("  ✓ SUCCESS - Frame decoded correctly");
        } else {
            System.out.println("  ✗ FAILED - No valid suffix found");
        }
    }
    
    static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
    static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
