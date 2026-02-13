package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class PstProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new PstProtocolDecoder(null));

        verifyPosition(decoder, binary(
                "2faf97de06000024db0551380cbb08070b040000015a0c09b50177e5100a1822da0d010d0f0451380628101451380cc384b800488a84036901b202d3010001061103ffff00150203523687"));

        verifyPosition(decoder, binary(
                "2fafb18306000005ab055166ee8f08080b0400008ec00c09b50022aef924d320f30d01130f045166ee8f10145166ee92805118978b59109900b3000b0000010c110300a7011404b58000001502039ff6e4"));

        verifyPosition(decoder, binary(
                "2faf9ab606000004c7055052ec88c0070b04000015050c09b500a25271c733e0720d01fe0f045052ec8410145052ba07858413918af325e7020802fe010001051103ffff0015023bbdc87d"));

        verifyPosition(decoder, binary(
                "2faf9ab606000004c7055052ec88c0070b04000015050c09b500a25271c733e0720d01fe0f045052ec8410145052ba07858413918af325e7020802fe010001051103ffff0015023bbdc87d"));

        verifyNull(decoder, binary(
                "2faf9b4c0600000012054f36ec194000bfa9"));

        verifyNull(decoder, binary(
                "2faf9b5605e40000e0022faf9b560196cb2f003f0c72ab56129ae0847ac98801cd1ed8"));

        verifyNull(decoder, binary(
                "2fafac5a050f0000e0022fafac5a01891e882bbfdd06dd577c9865620a0efe524c419f940b6710f5ba0c86e5868ffc97c77eaaf166a31dba63f9894e98a91b9486c94e79ce537359737a5e9385431a590eb20b5115a2b7939e4e66ae"));

    }

}
