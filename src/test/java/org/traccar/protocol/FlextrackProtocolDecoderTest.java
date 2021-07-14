package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;


public class FlextrackProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = new FlextrackProtocolDecoder(null);

        verifyNull(decoder, text(
                "-1,LOGON,7000000123,8945000000"));

        verifyNull(decoder, text(
                "-1,LOGON,1080424008,8945020110126633198"));

        verifyPosition(decoder, text(
                "-2,UNITSTAT,20060101,123442,1080424008,N0.00.0000,E0.00.0000,0,0,0,4129,-61,2,23866,0,999,A214,63,2EE2,3471676"));

        verifyPosition(decoder, text(
                "-2,UNITSTAT,20050205,181923,7000004634,N55.46.0812,E009.21.1665,122,198,6,3934,-81,01A8,23802,213,55,37FD,45,0055,12878"),
                position("2005-02-05 18:19:23.000", true, 55.76802, 9.35278));

        verifyPosition(decoder, text(
                "-2,UNITSTAT,20050205,181923,7000004634,N55.46.0812,E009.21.1665,122,198,6,3934,-81,01A8,23802,213,55,37FD,45,0055,12878"));

    }

}
