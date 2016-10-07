package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class FifotrackProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        FifotrackProtocolDecoder decoder = new FifotrackProtocolDecoder(new FifotrackProtocol());

        verifyPosition(decoder, text(
                "$$105,866104023179743,AB,A00,,161007085534,A,54.738791,25.271918,0,350,151,0,17929,0000,0,,246|1|65|96DB,936|0*0B"));

        verifyPosition(decoder, text(
                "$$103,866104023179743,5,A00,,161006192841,A,54.738791,25.271918,0,342,200,0,4265,0000,0,,246|1|65|96DB,9C4|0*75"));

        verifyPosition(decoder, text(
                "$$103,866104023179743,4,A00,,161006192810,V,54.738791,25.271918,0,158,122,0,4235,0000,0,,246|1|65|96DB,9C5|0*69"));

        verifyPosition(decoder, text(
                "$$135,866104023192332,29,A01,,160606093046,A,22.546430,114.079730,0,186,181,0,415322,0000,02,2,460|0|27B3|EA7,A2F|3B9|3|0,940C7E,31.76|30.98*46"));

    }

}
