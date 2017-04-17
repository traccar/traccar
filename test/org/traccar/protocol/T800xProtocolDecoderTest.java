package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class T800xProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        T800xProtocolDecoder decoder = new T800xProtocolDecoder(new T800xProtocol());

        verifyNull(decoder, binary(
                "232301001500000880316890202968140197625020"));

        verifyNull(decoder, binary(
                "232303000f00000880316890202968"));

        verifyAttributes(decoder, binary(
                "232302004200000880316890202968001e02582d00000000000000050000320000018901920000001dc1e2001601081154255d0202005a0053875a00a57e5a00af80"));

        verifyNull(decoder, binary(
                "232301001500020357367031063979150208625010"));

        verifyNull(decoder, binary(
                "232303000f00000357367031063979"));

        verifyPosition(decoder, binary(
                "232304004200030357367031063979003c03842307d00000c80000050100008000008900890100000017b100151022121648b8ef0c4422969342cec5944100000110"));

        verifyPosition(decoder, binary(
                "232302004200150357367031063979003c03842307d000004a0000050100004001009500940000000285ab001510281350477f710d4452819342d1ba944101160038"));

        verifyAttributes(decoder, binary(
                "232302004200000357367031063979003c03842307d000008000000501000000010094009400000002a0b90015102814590694015a00620cf698620cf49e620cf498"));

    }

}
