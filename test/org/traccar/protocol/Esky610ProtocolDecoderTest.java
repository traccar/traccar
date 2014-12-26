package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import java.net.InetSocketAddress;

public class Esky610ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Esky610ProtocolDecoder decoder = new Esky610ProtocolDecoder(new TestDataManager(), null, null, false);
        InetSocketAddress localhost = new InetSocketAddress("localhost", 5072);

        //////////////
        // Error cases

        // String too short
        assertNull(decoder.decode(null, null, localhost, "EL;0"));

        // Report message missing the IMEI (actual received message - why?)
        assertNull(decoder.decode(null, null, localhost,
                "EO;0;R;0+141007173437+0.00000+0.00000+0.00+0+0x1+24+5244299"));

        //////////////
        // Valid cases

        // Login Messages (they return null because they don't report position)
        assertNull(decoder.decode(null, null, localhost,
                "EL;1;123456789012345;141012034659;"));

        assertNull(decoder.decode(null, null, localhost,
                "EL;1;123456789012345;141012034703;"));

        assertNull(decoder.decode(null, null, localhost,
                "EL;1;123456789012345;141012034708;"));


        // Report Messages
        verify(decoder.decode(null, null, localhost,
                "EO;0;123456789012345;R;11+141007132350+42.17372+-76.31386+0.04+0+0x1+0+5243295"));

        verify(decoder.decode(null, null, localhost,
                "EO;0;123456789012345;R;11+141007132350+42.17372+76.31386+0.04+0+0x1+0+5243295"));

        verify(decoder.decode(null, null, localhost,
                "EO;0;123456789012345;R;11+141007132350+-42.17372+-76.31386+0.04+0+0x1+0+5243295"));

        verify(decoder.decode(null, null, localhost,
                "EO;0;123456789012345;R;11+141007132350+-42.17372+76.31386+0.04+0+0x1+0+5243295"));

        verify(decoder.decode(null, null, localhost,
                "EO;0;123456789012345;R;0+141007133211+0.00000+0.00000+0.00+0+0x1+24+5243295"));
 
        verify(decoder.decode(null, null, localhost,
                "EO;0;123456789012345;R;0+141007133211+0.00001+0.00000+0.00+0+0x1+24+5243295"));

        verify(decoder.decode(null, null, localhost,
                "EO;0;123456789012345;R;12+141007134408+42.17382+-76.31385+0.02+0+0x1+0+5243295"));

        verify(decoder.decode(null, null, localhost,
                "EO;0;123456789012345;R;12+141007135408+42.17381+-76.31389+0.01+0+0x1+0+5243295"));

        verify(decoder.decode(null, null, localhost,
                "EO;0;123456789012345;R;0+141007140228+0.00000+0.00000+0.00+0+0x1+24+5243295"));

        verify(decoder.decode(null, null, localhost,
                "EO;0;123456789012345;R;0+141007141427+0.00000+0.00000+0.00+0+0x1+0+5243295"));
    }

}
