package org.traccar.protocol;


import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.traccar.helper.TestDataManager;

import static org.junit.Assert.assertEquals;

public class Huabao808ProtocolDecoderTest {

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String bytesToString(byte[] bytes) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            sb.append(String.format("%02X", b));
        }

        return sb.toString();
    }

    @Test
    public void testTerminalAuthentication() throws Exception {

        Huabao808ProtocolDecoder decoder = new Huabao808ProtocolDecoder(new TestDataManager(), null, null);

        String msg = "7E010200100940278494700084323031313131303831313333323139369F7E";
        String response = "7E8001000509402784947000880084010200857E";


        decoder.decode(null, null, ChannelBuffers.wrappedBuffer(hexStringToByteArray(msg)));

        assertEquals(response,bytesToString(decoder.responseTerminalAuthentication()));
    }

    @Test
    public void testPlatformAuthentication() throws Exception {

        Huabao808ProtocolDecoder decoder = new Huabao808ProtocolDecoder(new TestDataManager(), null, null);

        String msg = "7e010000190940278494700012000000000000000000000000000000000000094027849470000a7e";
        String response = "7E810000130940278494700085001200323031313131303831313333323139360D7E";

        decoder.decode(null, null, ChannelBuffers.wrappedBuffer(hexStringToByteArray(msg)));

        assertEquals(response.substring(0,32), bytesToString(decoder.responsePlatformAuthentication()).substring(0,32));
    }

    @Test
    public void testLocation() throws Exception {

        Huabao808ProtocolDecoder decoder = new Huabao808ProtocolDecoder(new TestDataManager(), null, null);

        String msg = "7e0200002e094027587492000a000000010000000a03083db7001329f3000000140000130412164952010400000012360a0002341502cb0c20085c107e";
        String response = "7E800100050940275874920085000A020000D97E";

        decoder.decode(null, null, ChannelBuffers.wrappedBuffer(hexStringToByteArray(msg)));

        assertEquals(response, bytesToString(decoder.responseLocation()));
    }

    @Test
    public void testLocation1() throws Exception {

        Huabao808ProtocolDecoder decoder = new Huabao808ProtocolDecoder(new TestDataManager(), null, null);

        String msg ="7e020000220014012499170007000000000000400e012af16f02cbd2ba000000000000150101194257010400000077a97e";

        decoder.decode(null, null, ChannelBuffers.wrappedBuffer(hexStringToByteArray(msg)));
    }

}
