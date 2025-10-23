public class AnalyzeSystemLog {
    public static void main(String[] args) {
        String hex = "6100c91a1323582c3139313330363637352c3032302c30303037352c31393138342c4930323935302c30323935302c302c30363637352c534c3830383420537570706f7274204654502c41584e5f332e385f333333335f31363037313330302c303030322c5175656374656c2d4c38362c312e302a30320000000000002c4175746f300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005d2c303030302c302e30562c530059";
        
        System.out.println("=== Analyzing System Log Packet ===\n");
        System.out.println("Length: " + hex.length() / 2 + " bytes");
        
        // Parse header
        int type = Integer.parseInt(hex.substring(0, 2), 16);
        int seq = Integer.parseInt(hex.substring(2, 4), 16);
        int size = Integer.parseInt(hex.substring(4, 6), 16);
        int boxIdH = Integer.parseInt(hex.substring(6, 8), 16);
        int boxIdL = Integer.parseInt(hex.substring(8, 10), 16);
        int boxId = (boxIdH << 8) | boxIdL;
        
        System.out.println("Header:");
        System.out.println("  Type: 0x" + String.format("%02X", type) + " (" + type + ")");
        System.out.println("  Sequence: " + seq);
        System.out.println("  Size: " + size + " bytes");
        System.out.println("  BoxID: 0x" + String.format("%04X", boxId) + " (" + boxId + ")");
        System.out.println("  Device: " + (1300000 + boxId));
        
        // Data starts at position 10
        String data = hex.substring(10);
        System.out.println("\nData+CRC length: " + data.length() / 2 + " bytes");
        
        // Expected: size field says how many bytes (including CRC)
        // Actual data = size - 2 (for CRC)
        int expectedDataLength = size - 2;
        int actualDataLength = data.length() / 2 - 2; // minus CRC
        
        System.out.println("Expected data length (from size field): " + expectedDataLength + " bytes");
        System.out.println("Actual data length: " + actualDataLength + " bytes");
        
        // Get CRC from end
        String crcInPacket = data.substring(data.length() - 4);
        System.out.println("\nCRC in packet: 0x" + crcInPacket);
        
        // Get data without CRC
        String dataOnly = data.substring(0, data.length() - 4);
        System.out.println("Data (without CRC): " + dataOnly);
        System.out.println("Data length: " + dataOnly.length() / 2 + " bytes");
        
        // Try to decode as ASCII
        System.out.println("\n=== Data as ASCII ===");
        try {
            StringBuilder ascii = new StringBuilder();
            for (int i = 0; i < dataOnly.length(); i += 2) {
                int val = Integer.parseInt(dataOnly.substring(i, i + 2), 16);
                if (val >= 32 && val <= 126) {
                    ascii.append((char) val);
                } else {
                    ascii.append(".");
                }
            }
            System.out.println(ascii.toString());
        } catch (Exception e) {
            System.out.println("Error decoding ASCII");
        }
        
        // Check if there are trailing zeros
        System.out.println("\n=== Trailing Data Analysis ===");
        String last80 = dataOnly.substring(Math.max(0, dataOnly.length() - 80));
        System.out.println("Last 40 bytes: " + last80);
        
        // Count trailing zeros
        int zeroCount = 0;
        for (int i = dataOnly.length() - 2; i >= 0; i -= 2) {
            String byteStr = dataOnly.substring(i, i + 2);
            if (byteStr.equals("00")) {
                zeroCount++;
            } else {
                break;
            }
        }
        System.out.println("Trailing zero bytes: " + zeroCount);
        
        // Find meaningful data end
        int meaningfulEnd = dataOnly.length();
        for (int i = dataOnly.length() - 2; i >= 0; i -= 2) {
            String byteStr = dataOnly.substring(i, i + 2);
            if (!byteStr.equals("00")) {
                meaningfulEnd = i + 2;
                break;
            }
        }
        
        String meaningfulData = dataOnly.substring(0, meaningfulEnd);
        System.out.println("\nMeaningful data length: " + meaningfulData.length() / 2 + " bytes");
        System.out.println("Padding: " + (dataOnly.length() - meaningfulData.length()) / 2 + " bytes");
        
        System.out.println("\n=== Issue ===");
        System.out.println("Type 0x" + String.format("%02X", type) + " is not 0x61 (97 = M_SYSTEM_LOG)");
        System.out.println("It's 0x" + String.format("%02X", type) + " which indicates frame delimiter!");
        System.out.println("This means the packet still has preamble/suffix issues.");
    }
}
