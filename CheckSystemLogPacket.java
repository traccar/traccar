public class CheckSystemLogPacket {
    public static void main(String[] args) {
        // Full packet with framing
        String full = "7e7e7e7e006100c91a1323582c3139313330363637352c3032302c30303037352c31393138342c4930323935302c30323935302c302c30363637352c534c3830383420537570706f7274204654502c41584e5f332e385f333333335f31363037313330302c303030322c5175656374656c2d4c38362c312e302a30320000000000002c4175746f300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005d2c303030302c302e30562c530059867e";
        
        System.out.println("=== Full Packet Analysis ===");
        System.out.println("Length: " + full.length() / 2 + " bytes\n");
        
        // Check preamble
        String preamble = full.substring(0, 10);
        System.out.println("Preamble: " + preamble);
        if (preamble.equals("7e7e7e7e00")) {
            System.out.println("✓ Valid preamble");
        }
        
        // Check suffix - look for last occurrence of 7e7e or 0000
        int len = full.length();
        String last4 = full.substring(len - 4);
        System.out.println("Last 4 chars: " + last4);
        
        // Find 867e at the end
        System.out.println("\n=== Suffix Problem ===");
        System.out.println("Expected suffix: 7E7E or 0000");
        System.out.println("Actual ending: " + last4);
        
        if (last4.equals("867e")) {
            System.out.println("⚠ Suffix is 867E, not a valid frame delimiter!");
            System.out.println("This means:");
            System.out.println("  - 0x86 is part of data or CRC");
            System.out.println("  - 0x7E is partial suffix (should be 7E7E)");
        }
        
        // Try to find where data should end
        System.out.println("\n=== Finding Actual Suffix ===");
        
        // Look for 7e7e or 0000 patterns
        for (int i = len - 10; i < len - 3; i += 2) {
            String twoBytes = full.substring(i, i + 4);
            if (twoBytes.equals("7e7e") || twoBytes.equals("0000")) {
                System.out.println("Found potential suffix at position " + (i/2) + ": " + twoBytes);
            }
        }
        
        // The real issue: packet ends with 0059867e
        // 0059 = could be CRC
        // 86 = data?
        // 7e = partial suffix
        
        System.out.println("\n=== Last bytes breakdown ===");
        String last20 = full.substring(full.length() - 20);
        System.out.println("Last 10 bytes: " + last20);
        for (int i = 0; i < 20; i += 2) {
            String b = last20.substring(i, i + 2);
            System.out.println("  [" + (i/2) + "]: 0x" + b);
        }
        
        System.out.println("\n=== Conclusion ===");
        System.out.println("The packet is missing proper 7E7E suffix!");
        System.out.println("It ends with: ...0059 86 7E");
        System.out.println("Should end with: ...CRC 7E 7E");
        System.out.println("\nOptions:");
        System.out.println("1. Device firmware issue - not sending proper suffix");
        System.out.println("2. Network truncation - last 7E is lost");
        System.out.println("3. Frame decoder should handle single 7E as suffix");
    }
}
