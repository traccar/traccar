package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class ContinentalProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new ContinentalProtocolDecoder(null));

        verifyPosition(decoder, binary(
                "5356003216001eb48505025b4001e90f7f18ce0f00522200400001015b4001e9000e820100000c24000100014e0400736a7a"),
                position("2018-07-06 23:57:29.000", true, -23.46609, -46.54497));

        verifyPosition(decoder, binary(
                "5356002A1100003039030243A68B5700FEB5AB00FD715F012700000143A68B57000E000000000C2F00000130"),
                position("2005-12-19 10:28:39.000", true, -23.49027, -46.55138));

        verifyPosition(decoder, binary(
                "5356002a0d0010a12403025a9ea47f00feb48400fd6e63000c0000015a9ea480000e000100000c000000"));

        verifyPosition(decoder, binary(
                "5356002a0d0010a1240302581b944100febed800fd9fa30139001300581c73fa000e000000000d000001"));

    }

}
