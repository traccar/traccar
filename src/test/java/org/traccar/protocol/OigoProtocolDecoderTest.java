package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class OigoProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new OigoProtocolDecoder(null);

        verifyPosition(decoder, binary(
                "7e002e000000146310002523830400001bfb000369150f310c0591594d062ac0c0141508011303cd63101604fd00000000"));

        verifyPosition(decoder, binary(
                "0103537820628365110310410790660962521813380026EE4EFF8593AA0065003E00794C020600100500000000"));

        verifyPosition(decoder, binary(
                "0E03537820628344660204043255862749531B100E0026EE3AFF8593A3FFFE00BF00044C20090710C300000000"));

        verifyPosition(decoder, binary(
                "00035378206638500203340201271426226b190203001ac000ff72eedd00370097238b4c34116a130b000094d9"));

        verifyPosition(decoder, binary(
                "1d035378206638500203340201271426226b19020c001ab144ff72f74d005f0097298a4c1d066d130b000094de"));

        verifyPosition(decoder, binary(
                "00035378206638500203340201271426226b191016001c04e5ff760081013d002900814c1a0f5e130b00009576"));

        verifyPosition(decoder, binary(
                "7e004200000014631000258257000000ffff02d0690e000220690e0002200696dbd204bdfde31a070000b307101135de106e05f500000000010908010402200104ffff8001"));

        verifyPosition(decoder, binary(
                "7e004200000014631000258257000000ffff02d1690e00051f690e00051f0696dbd204bdfde31a070000b307100f35c0106305f500000000010908010402200104ffff8001"));

        verifyPosition(decoder, binary(
                "7e004200000014631000258257000000ffff0d82691300001669130000160696dbd804bdfdbb1a0800000007101035a2106905f500000000010908010402200104ffff8001"));

    }

}
