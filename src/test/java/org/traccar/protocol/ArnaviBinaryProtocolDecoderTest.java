package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class ArnaviBinaryProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testHeader1Decode() throws Exception {

        var decoder = inject(new ArnaviBinaryProtocolDecoder(null));

        verifyNull(decoder, binary(
                "ff22f30c45f5c90f0300"));

        verifyPositions(decoder, binary(
                "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d"),
                position("2017-07-07 05:09:55.000", true, 45.05597, 39.03347));
    }

    @Test
    public void testHeader2Decode() throws Exception {
        var decoder = inject(new ArnaviBinaryProtocolDecoder(null));
        verifyNull(decoder, binary(
                "ff23f30c45f5c90f0300"));
        verifyPositions(decoder, binary(
                "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d"),
                position("2017-07-07 05:09:55.000", true, 45.05597, 39.03347));
    }

    @Test
    public void testHeader3Decode() throws Exception {
        var decoder = inject(new ArnaviBinaryProtocolDecoder(null));
        verifyNull(decoder, binary("ff24f30c45f5c90f03000102030405060708"));
        verifyPositions(decoder, binary(
                "5b01012800a3175f5903513934420447221c42055402781E0900f0c5215b4e0084005c00007c005d0000a300fa37010000295d"),
                position("2017-07-07 05:09:55.000", true, 45.05597, 39.03347));
    }

    @Test
    public void testDecode() throws Exception {
        var decoder = inject(new ArnaviBinaryProtocolDecoder(null));

        verifyNull(decoder, binary("ff23ae52a969f30c45f5"));

        verifyPositions(decoder, binary(
                "5bae0196008a52a9690373f35c42049746ac420574147529973e00000007ca325cb061ca3200000863fa001e96e553fe006353801a0201b50f5d6c0601010000474e0000005b4e0000005c000017005d000017003203000000330040110034d83b05003579b7200136700e090037c301000039ce0300003a4c0000003b4e0000003c8a0101003db8ab0000404c010000be1e4b00c045002d0000fa3b010000d75d"),
                position("2026-03-05 09:53:14.000", true, 55.237743, 86.137871));

        verifyAttribute(decoder, binary(
                "5bae0196008a52a9690373f35c42049746ac420574147529973e00000007ca325cb061ca3200000863fa001e96e553fe006353801a0201b50f5d6c0601010000474e0000005b4e0000005c000017005d000017003203000000330040110034d83b05003579b7200136700e090037c301000039ce0300003a4c0000003b4e0000003c8a0101003db8ab0000404c010000be1e4b00c045002d0000fa3b010000d75d"),
                Position.KEY_RPM, 974);

        verifyAttribute(decoder, binary(
                "5bae0196008a52a9690373f35c42049746ac420574147529973e00000007ca325cb061ca3200000863fa001e96e553fe006353801a0201b50f5d6c0601010000474e0000005b4e0000005c000017005d000017003203000000330040110034d83b05003579b7200136700e090037c301000039ce0300003a4c0000003b4e0000003c8a0101003db8ab0000404c010000be1e4b00c045002d0000fa3b010000d75d"),
                Position.KEY_HOURS, 12348000000L);

        verifyAttribute(decoder, binary(
                "5bae0196008a52a9690373f35c42049746ac420574147529973e00000007ca325cb061ca3200000863fa001e96e553fe006353801a0201b50f5d6c0601010000474e0000005b4e0000005c000017005d000017003203000000330040110034d83b05003579b7200136700e090037c301000039ce0300003a4c0000003b4e0000003c8a0101003db8ab0000404c010000be1e4b00c045002d0000fa3b010000d75d"),
                Position.KEY_ODOMETER, 189213370L);
    }
}