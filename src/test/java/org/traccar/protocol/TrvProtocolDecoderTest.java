package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class TrvProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new TrvProtocolDecoder(null));

        verifyNull(decoder, text(
                "TRVAP00352121088015548"));

        verifyPosition(decoder, text(
                "TRVYP03190805A1828.9242N07353.9423E000.0150716029.0010000810020201112,404,27,184,10229"));

        verifyNotNull(decoder, text(
                "IWAP02,zh_cn,0,6,260,1,11002|39252|9,11002|35112|23,11002|11043|24,11002|39253|24,11002|13751|24,11018|8102|26,3,a|c0-4a-00-b6-9c-f5|64&a|c0-4a-00-b6-9c-f5|64&a|18-a6-f7-92-35-da|84"));

        verifyPosition(decoder, text(
                "TRVAP01170905A5227.1382N00541.4256E001.7095844000.0008100610020100,204,8,3230,13007"));

        verifyAttributes(decoder, text(
                "TRVCP01,07800010010000602001206001120124"));

        verifyNull(decoder, text(
                "IWAP00353456789012345"));

        verifyPosition(decoder, text(
                "IWAP01080524A2232.9806N11404.9355E000.1061830323.8706000908000102,460,0,9520,3671,Home|74-DE-2B-44-88-8C|97& Home1|74-DE-2B-44-88-8C|97&Home2|74-DE-2B-44-88-8C|97& Home3|74-DE-2B-44-88-8C|97"));

        verifyNotNull(decoder, text(
                "IWAP02,zh_cn,0,7,460,0,9520|3671|13,9520|3672|12,9520|3673|11,9520|3674|10,9520|3675|9,9520|3676|8,9520|3677|7,4,1|D8-24-BD-79-FA-1F|59&2|3C-46-D8-6D-CE-01|81&3|0C-4C-39-1A-7C-65|69&4|70-A8-E3-5D-D7-C0|65"));

        verifyPosition(decoder, text(
                "IWAP10080524A2232.9806N11404.9355E000.1061830323.8706000908000502,460,0,9520,3671,00,zh-cn,00,HOME|74-DE-2B-44-88-8C|97&HOME1|74-DE-2B-44-88-8C|97&HOME2|74-DE-2B-44-88-8C|97&HOME3|74-DE-2B-44-88-8C|97"));

        verifyNull(decoder, text(
                "IWAP03,06000908000102,5555,30"));

        verifyNull(decoder, text(
                "TRVAP00353456789012345"));

        verifyAttributes(decoder, text(
                "TRVCP01,06000908000102"));

        verifyAttributes(decoder, text(
                "TRVCP01,100007100000001020151060011"));

        verifyPosition(decoder, text(
                "TRVAP01160211A2544.5118N05553.7586E105.711185941.52010001010010000,424,030,3011,27003"));

        verifyPosition(decoder, text(
                "TRVAP01160209A2540.8863N05546.6125E005.6075734123.7910000810010000,424,030,3012,27323"));

        verifyPosition(decoder, text(
                "TRVAP01080524A2232.9806N11404.9355E000.1061830323.8706000908000102,460,0,9520,3671"));

        verifyPosition(decoder, text(
                "TRVAP01080524A2232.9806N11404.9355E000.1061830323.8706000908000102,460,0,9520,3671"),
                position("2008-05-24 06:18:30.000", true, 22.54968, 114.08226));

        verifyPosition(decoder, text(
                "TRVAP10080524A2232.9806N11404.9355E000.1061830323.8706000908000502,460,0,9520,3671,00,zh-cn,00"));

        verifyPosition(decoder, text(
                        "TRVYP14220217A5235.7885N00724.1840E000.0130919177.561000050660000200004,262,01,14635,52789,FritzBox7|DC-39-8F-7E-94-73|-89&FritzBox7|24-4E-5D-71-C3-9C|-90&MY_IOT|80-B4-F7-77-9C-7C|-81&MYAP|44-D4-F7-77-9C-7C|-80#"));

    }

}
