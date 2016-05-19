package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class CguardDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        CguardProtocolDecoder decoder = new CguardProtocolDecoder(new CguardProtocol());

        /*HEX: 56455253494f4e3a332e330a4944524f3a3335343836383035303635353238330a
        VERSION:3.3
        IDRO:354868050655283*/

        verifyNothing(decoder, binary("56455253494f4e3a332e330a4944524f3a3335343836383035303635353238330a"));

        verifyPosition(decoder, binary(               "4e563a313630343230203130313930323a35352e3739393432353a33372e3637343033333a302e39343a4e414e3a3231332e35393a3135362e360a4e563a313630343230203130313930333a35352e3739312e32383a4e414e3a3335392e31363a3135362e360a42433a313630343230203130313930333a435351313a37343a4e5351313a31303a424154313a3130300a"), position("2016-04-20 10:19:02.000", true, 55.799425, 37.674033));

        /*NV:160420 101902:55.799425:37.674033:0.94:NAN:213.59:156.6
        BC:160420 101903:CSQ1:74:NSQ1:10:BAT1:100*/
    }

}
