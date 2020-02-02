package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

import java.nio.ByteOrder;

public class Arnavi4ProtocolDecoderTest extends ProtocolTest {

    /*@Test
    public void testHeader1Decode() throws Exception {

        Arnavi4ProtocolDecoder decoder;

        decoder = new Arnavi4ProtocolDecoder(new Arnavi4Protocol());

        verifyNull(decoder, binary(ByteOrder.LITTLE_ENDIAN, // Valid HEADER v1 packet with IMEI
                "ff22f30c45f5c90f0300"));

        verifyPositions(decoder, binary(ByteOrder.LITTLE_ENDIAN, // Valid PACKAGE packet with one DATA packet
                "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d"),
                position("2017-07-07 05:09:55.000", true, 45.05597, 39.03347));
    }

    @Test
    public void testHeader2Decode() throws Exception {

        Arnavi4ProtocolDecoder decoder;

        decoder = new Arnavi4ProtocolDecoder(new Arnavi4Protocol());

        verifyNull(decoder, binary(ByteOrder.LITTLE_ENDIAN, // Valid HEADER v2 packet with IMEI
                "ff23f30c45f5c90f0300"));

        verifyPositions(decoder, binary(ByteOrder.LITTLE_ENDIAN, // Valid PACKAGE packet with two DATA packet
                "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa3701000029012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d"),
                position("2017-07-07 05:09:55.000", true, 45.05597, 39.03347));
    }*/

}
