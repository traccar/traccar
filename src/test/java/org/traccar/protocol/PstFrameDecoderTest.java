package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class PstFrameDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new PstFrameDecoder();

        verifyFrame(
                binary("2fafac5a050f0000e0022fafac5a01891e882bbfdd06dd577c9865620a0efe524c419f940b6710f5ba0c86e5868ffc97c77eaaf166a31dba63f9894e98a91b9486c94e79ce537359737a5e9385431a590eb20b5115a2b7939e4e66ae"),
                decoder.decode(null, null, binary("282fafac5a050f0000e0022fafac5a01891e882bbfdd06dd577c9865620a0efe524c419f940b6710f5ba0c86e5868ffc97c77eaaf166a31dba63f9894e98a91b9486c94e79ce537359737a5e9385431a590eb20b5115a2b7939e4e66ae29")));

    }

}
