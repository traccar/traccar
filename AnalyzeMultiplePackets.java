public class AnalyzeMultiplePackets {
    
    public static void main(String[] args) {
        String[] packets = {
            "7208240fb011f1060b335721ae03c1d2183bc148afff050012280f000000000878000150007000df548e",
            "7201240e5a11e00a000c6921ae07f168b63b5688effe050008271f0000000008006b00000070b688ef28",
            "720324156311f50604335721af0891008e3c3b5772ff050010283d0000000008700004101070008bda44",
            "7207241abf11e00700335721af0a439b623d16ad90ff05000e29b307c00000086c000e00007000b27cf6"
        };
        
        System.out.println("=== Analyze Multiple GTR-9 Packets ===\n");
        
        for (int p = 0; p < packets.length; p++) {
            String hex = packets[p];
            byte[] data = hexStringToByteArray(hex);
            
            System.out.println("--- Packet " + (p + 1) + " ---");
            System.out.println("Hex: " + hex);
            System.out.println("Length: " + data.length + " bytes");
            
            // Parse header
            int type = data[0] & 0xFF;
            int seq = data[1] & 0xFF;
            int size = data[2] & 0xFF;
            int boxId = ((data[3] & 0xFF) << 8) | (data[4] & 0xFF);
            
            System.out.println("Type: 0x" + String.format("%02X", type) + " (" + type + ")");
            System.out.println("Seq: " + seq);
            System.out.println("Size: " + size + " bytes");
            System.out.println("BoxID: 0x" + String.format("%04X", boxId));
            System.out.println("Actual length: " + data.length + " bytes");
            System.out.println("Difference: " + (data.length - size) + " bytes");
            
            // Try CRC at different positions
            System.out.println("\nCRC Analysis:");
            
            // Position 1: Last 2 bytes
            int pos1 = data.length - 2;
            int crc1Recv = ((data[pos1] & 0xFF) << 8) | (data[pos1 + 1] & 0xFF);
            int crc1Calc = calculateCrc16(data, 0, pos1);
            System.out.println("  Last 2 bytes [" + pos1 + "-" + (pos1+1) + "]:");
            System.out.println("    Received: 0x" + String.format("%04X", crc1Recv));
            System.out.println("    Calculated: 0x" + String.format("%04X", crc1Calc));
            System.out.println("    Match: " + (crc1Recv == crc1Calc ? "✅" : "❌"));
            
            // Position 2: Size - 2
            int pos2 = size - 2;
            if (pos2 > 0 && pos2 < data.length - 1) {
                int crc2Recv = ((data[pos2] & 0xFF) << 8) | (data[pos2 + 1] & 0xFF);
                int crc2Calc = calculateCrc16(data, 0, pos2);
                System.out.println("  At Size-2 [" + pos2 + "-" + (pos2+1) + "]:");
                System.out.println("    Received: 0x" + String.format("%04X", crc2Recv));
                System.out.println("    Calculated: 0x" + String.format("%04X", crc2Calc));
                System.out.println("    Match: " + (crc2Recv == crc2Calc ? "✅" : "❌"));
            }
            
            // GPS32 record (32 bytes starting at position 5)
            System.out.println("\nGPS32 Record (bytes 5-36):");
            if (data.length >= 37) {
                for (int i = 5; i < 37; i++) {
                    System.out.printf("%02X ", data[i] & 0xFF);
                    if ((i - 4) % 16 == 0) System.out.println();
                }
            }
            
            // RecordCRC is at position 36 (last byte of GPS32)
            if (data.length > 36) {
                int recordCrc = data[36] & 0xFF;
                int calcRecordCrc = calculateRecordCrc(data, 5, 31); // GPS32 without last byte
                System.out.println("\n  GPS32 RecordCRC: 0x" + String.format("%02X", recordCrc) + 
                                   " (calculated: 0x" + String.format("%02X", calcRecordCrc) + ")");
            }
            
            // Bytes after GPS32
            if (data.length > 37) {
                System.out.println("\nBytes after GPS32 (position 37+):");
                for (int i = 37; i < data.length; i++) {
                    System.out.printf("[%d]=0x%02X ", i, data[i] & 0xFF);
                }
                System.out.println();
            }
            
            System.out.println("\n");
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
    
    private static int calculateRecordCrc(byte[] data, int offset, int length) {
        int crc = 0;
        for (int i = offset; i < offset + length; i++) {
            crc += data[i] & 0xFF;
        }
        return crc & 0xFF;
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
