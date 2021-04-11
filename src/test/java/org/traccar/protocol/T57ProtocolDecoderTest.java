package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class T57ProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new T57ProtocolDecoder(null);

        verifyPosition(decoder, text(
                "*T57#F1#T571117001#301117#000843#2234.1303#N#08826.1714#E#+0.242,+0.109,-0.789#0.000#6.20000#A2#4.2#"));

        verifyPosition(decoder, text(
                "*T57#F1#0123456789#041117#152900#1258.9653#N#07738.4169#E#00000000000000000000#0.000#926.300#A2#4.0#"));

        verifyPosition(decoder, text(
                "*T57#F2#0123456789#041117#152900#1258.9653#N#07738.4169#E#00000000000000000000#0.000#926.300#A2#4.0#"));

        verifyNull(decoder, text(
                "*T57#F3#0123456789#041117#152900#1258.9653#N#07738.4169#E#I#9674432345#340#"));

    }

}
