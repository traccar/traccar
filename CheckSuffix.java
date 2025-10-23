public class CheckSuffix {
    public static void main(String[] args) {
        String hex = "7e7e7e7e0073092410ab11e005003357110303c278c63bc19e82fe050012282d00000000079800000000700028746b0000";
        
        System.out.println("Packet: " + hex);
        System.out.println("Length: " + hex.length() / 2 + " bytes\n");
        
        // Find all 7E positions
        System.out.println("=== Finding all 7E bytes ===");
        for (int i = 0; i < hex.length(); i += 2) {
            String byteStr = hex.substring(i, i + 2);
            if (byteStr.equalsIgnoreCase("7E")) {
                System.out.println("Position " + (i/2) + ": 0x7E");
            }
        }
        
        // Check if there's a trailing 7E7E
        System.out.println("\n=== Checking suffix ===");
        if (hex.endsWith("7e7e") || hex.endsWith("7E7E")) {
            System.out.println("✓ Has proper suffix 7E7E");
        } else {
            System.out.println("✗ NO suffix 7E7E found!");
            System.out.println("Last 4 bytes: " + hex.substring(hex.length() - 8));
        }
        
        // What frame decoder would see
        System.out.println("\n=== Frame Decoder Processing ===");
        System.out.println("Preamble (0-4): " + hex.substring(0, 10));
        
        // Find first 7E7E after position 10
        int suffixPos = -1;
        for (int i = 10; i < hex.length() - 2; i += 2) {
            if (hex.substring(i, i+4).equalsIgnoreCase("7e7e")) {
                suffixPos = i / 2;
                System.out.println("Suffix found at byte position: " + suffixPos);
                break;
            }
        }
        
        if (suffixPos == -1) {
            System.out.println("⚠ NO SUFFIX FOUND - Frame decoder would wait for more data");
            System.out.println("This causes the decoder to NOT process the packet!");
        } else {
            String payload = hex.substring(10, suffixPos * 2);
            System.out.println("\nPayload (after removing preamble and suffix):");
            System.out.println(payload);
            System.out.println("Payload length: " + payload.length() / 2 + " bytes");
        }
    }
}
