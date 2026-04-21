package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class AisProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new AisProtocolDecoder(null));

        verifyPositions(decoder, text(
                "!AIVDM,2,1,8,A,53UlSb01l>Ei=H4KF218PTpv222222222222221?8h=766gB0<Ck11DTp888,0*14s:MTb827ebc7686b,c:1481688227737*4d\\\r\n" +
                "!AIVDM,2,2,8,A,88888888888,2*24\r\n" +
                "!AIVDM,1,1,,A,13T=Qr0P001cmmLEf;A00?wN0PSU,0*29\r\n" +
                "!AIVDM,1,1,,A,35Qf023Ohi1n5gdDRLW5FSQP00u@,0*18\r\n" +
                "!AIVDM,1,1,,A,B3P@f>0000K6J;5KAIT03wpUkP06,0*5D\\s:MTb827ebc7686b,c:1481688230418*45\\\r\n" +
                "!AIVDM,1,1,,B,B52Q8a@00Ul`9N5@ssbmCwr5oP06,0*36\r\n" +
                "!AIVDM,1,1,,A,1815<S@01VQnKGlE0sk:WHcT0@O4,0*78\r\n" +
                "!AIVDM,1,1,,A,35N7G;5OhQG?oJfE`G`cM9E`0001,0*6C\r\n" +
                "!AIVDM,1,1,,B,13Ug;r0P011cqHJEevuEiOwf0L3h,0*6A\r\n" +
                "!AIVDM,1,1,,A,13MKsr?0001dJC2Ee4W;jnal08Qj,0*00\r\n\r\n"));

        verifyPositions(decoder, text(
                "!AIVDM,1,1,,A,H3FUli4T000000000000001p0400,0*6E\\s:MTb827eba584a8,c:1481688176110*46\\\r\n" +
                "!AIVDM,1,1,,B,13UhUh0P01QcoRTEdtB>4?v<2D=j,0*54\r\n\r\n"));

    }

}
