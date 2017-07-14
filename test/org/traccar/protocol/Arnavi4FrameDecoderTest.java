package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;

import java.nio.ByteOrder;

public class Arnavi4FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Arnavi4FrameDecoder decoder = new Arnavi4FrameDecoder();

        Assert.assertEquals( // Valid HEADER v1 packet with IMEI
                binary(ByteOrder.LITTLE_ENDIAN, "ff22f30c45f5c90f0300"),
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "ff22f30c45f5c90f0300")));

        Assert.assertEquals( // Valid HEADER v2 packet with IMEI
                binary(ByteOrder.LITTLE_ENDIAN, "ff23f30c45f5c90f0300"),
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "ff23f30c45f5c90f0300")));

        Assert.assertEquals( // Valid PACKAGE packet with one DATA packet
                binary(ByteOrder.LITTLE_ENDIAN, "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d"),
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d")));

        Assert.assertEquals( // Valid PACKAGE packet with two DATA packet
                binary(ByteOrder.LITTLE_ENDIAN, "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa3701000029012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d"),
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa3701000029012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d")));

    }

}