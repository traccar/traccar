package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Arnavi4FrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecodeValidPackets() throws Exception {

        Arnavi4FrameDecoder decoder = new Arnavi4FrameDecoder();

        verifyFrame(
                binary("ff22f30c45f5c90f0300"),
                decoder.decode(null, null, binary("ff22f30c45f5c90f0300")));

        verifyFrame(
                binary("5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d"),
                decoder.decode(null, null, binary("5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d")));

        verifyFrame(
                binary("5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa3701000029012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d"),
                decoder.decode(null, null, binary("5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa3701000029012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d")));

        verifyFrame(
                binary("5b01030700e3f16b50747261636361721b5d"),
                decoder.decode(null, null, binary("5b01030700e3f16b50747261636361721b5d")));

        verifyFrame(
                binary("5b01030700e3f16b50747261636361721b030700e3f16b50747261636361721b5d"),
                decoder.decode(null, null, binary("5b01030700e3f16b50747261636361721b030700e3f16b50747261636361721b5d")));

        verifyFrame(
                binary("5b01061400e3f16b5003298b5e4204cbd514420500191000080400ff021b5d"),
                decoder.decode(null, null, binary("5b01061400e3f16b5003298b5e4204cbd514420500191000080400ff021b5d")));

        verifyFrame(
                binary("5b01061400e3f16b5003298b5e4204cbd514420500191000080400ff021b061400e3f16b5003298b5e4204cbd514420500191000080400ff021b5d"),
                decoder.decode(null, null, binary("5b01061400e3f16b5003298b5e4204cbd514420500191000080400ff021b061400e3f16b5003298b5e4204cbd514420500191000080400ff021b5d")));

        verifyFrame(
                binary("5bfd005d"),
                decoder.decode(null, null, binary("5bfd005d")));

    }

}
