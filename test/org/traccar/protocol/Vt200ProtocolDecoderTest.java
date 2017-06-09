package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class Vt200ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        Vt200ProtocolDecoder decoder = new Vt200ProtocolDecoder(new Vt200Protocol());

        verifyPosition(decoder, binary(
                "28631037309456208400340102dc0906171616454415760201144494473f920a0c0000030500200100417c1f383a9d1090510000006a00007000000e00180ee129"));

        verifyPosition(decoder, binary(
                "28631037309456208400340102dc090617161654441577230114439597368c0a0c0000030500200100417c1baa349d3290510000006a00007000003d15004c11c629"));

    }

}
