package org.traccar.protocol;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.traccar.helper.DecoderVerifier.verify;

public class FlextrackProtocolDecoderTest extends ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        FlextrackProtocolDecoder decoder = new FlextrackProtocolDecoder(new FlextrackProtocol());

        assertNull(decoder.decode(null, null,
                "-1,LOGON,7000000123,8945000000"));

        assertNull(decoder.decode(null, null,
                "-1,LOGON,1080424008,8945020110126633198"));

        verify(decoder.decode(null, null,
                "-2,UNITSTAT,20060101,123442,1080424008,N0.00.0000,E0.00.0000,0,0,0,4129,-61,2,23866,0,999,A214,63,2EE2,3471676"));

        verify(decoder.decode(null, null,
                "-2,UNITSTAT,20050205,181923,7000004634,N55.46.0812,E009.21.1665,122,198,6,3934,-81,01A8,23802,213,55,37FD,45,0055,12878"));

    }

}
