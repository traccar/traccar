package org.traccar.protocol;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.ProtocolTest;

import java.nio.ByteOrder;

public class Arnavi4FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecodeValidPackets() throws Exception {

        Arnavi4FrameDecoder decoder = new Arnavi4FrameDecoder();

        Assert.assertEquals( // Valid HEADER v1 packet with IMEI
                binary(ByteOrder.LITTLE_ENDIAN, "ff22f30c45f5c90f0300"),
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "ff22f30c45f5c90f0300")));

        Assert.assertEquals( // Valid PACKAGE with one DATA packet
                binary(ByteOrder.LITTLE_ENDIAN, "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d"),
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d")));

        Assert.assertEquals( // Valid PACKAGE with two DATA packet
                binary(ByteOrder.LITTLE_ENDIAN, "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa3701000029012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d"),
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa3701000029012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d")));

        Assert.assertEquals( // Valid PACKAGE with one TEXT packet.
                binary(ByteOrder.LITTLE_ENDIAN, "5b01030700e3f16b50747261636361721b5d"),
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "5b01030700e3f16b50747261636361721b5d")));

        Assert.assertEquals( // Valid PACKAGE with two TEXT packet.
                binary(ByteOrder.LITTLE_ENDIAN, "5b01030700e3f16b50747261636361721b030700e3f16b50747261636361721b5d"),
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "5b01030700e3f16b50747261636361721b030700e3f16b50747261636361721b5d")));

        Assert.assertEquals( // Valid PACKAGE with one BINARY packet.
                binary(ByteOrder.LITTLE_ENDIAN, "5b01061400e3f16b5003298b5e4204cbd514420500191000080400ff021b5d"),
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "5b01061400e3f16b5003298b5e4204cbd514420500191000080400ff021b5d")));

        Assert.assertEquals( // Valid PACKAGE with two BINARY packet.
                binary(ByteOrder.LITTLE_ENDIAN, "5b01061400e3f16b5003298b5e4204cbd514420500191000080400ff021b061400e3f16b5003298b5e4204cbd514420500191000080400ff021b5d"),
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "5b01061400e3f16b5003298b5e4204cbd514420500191000080400ff021b061400e3f16b5003298b5e4204cbd514420500191000080400ff021b5d")));

        Assert.assertEquals( // Valid PACKAGE with answer to server on file transfer
                binary(ByteOrder.LITTLE_ENDIAN, "5bfd005d"),
                decoder.decode(null, null, binary(ByteOrder.LITTLE_ENDIAN, "5bfd005d")));

    }

}