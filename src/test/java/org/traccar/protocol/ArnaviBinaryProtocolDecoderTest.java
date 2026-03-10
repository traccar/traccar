package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import io.netty.buffer.ByteBuf;

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
        public void testFullTagsDecode() throws Exception {
                var decoder = inject(new ArnaviBinaryProtocolDecoder(null));

                verifyNull(decoder, binary("ff23ae52a969f30c45f5"));

                verifyPositions(decoder, binary(
                                "5bae0191008a52a969" +
                                                "0373f35c42" +
                                                "049746ac42" +
                                                "0574147529" +
                                                "973e000000" +
                                                "07ca325cb0" +
                                                "61ca320000" +
                                                "0863fa001e" +
                                                "96e553fe00" +
                                                "6353801a02" +
                                                "01b50f5d6c" +
                                                "0601010000" +
                                                "5b4e000000" +
                                                "5c00001700" +
                                                "5d00001700" +
                                                "3203000000" +
                                                "3300401100" +
                                                "34d83b0500" +
                                                "3579b72001" +
                                                "36700e0900" +
                                                "37c3010000" +
                                                "39ce030000" +
                                                "3a4c000000" +
                                                "3b4e000000" +
                                                "3c8a010100" +
                                                "3db8ab0000" +
                                                "404c010000" +
                                                "be1e4b00c0" +
                                                "45002d0000" +
                                                "fa3b010000" +
                                                "d7" +
                                                "5d"),
                                position("2026-03-05 09:53:14.000", true, 55.237743, 86.137871));
        }
}