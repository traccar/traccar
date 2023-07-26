package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GatorProtocolDecoderTest extends ProtocolTest {
    
    @Test
    public void testDecodeId() {
        
        assertEquals("3512345006", GatorProtocolDecoder.decodeId(12, 162, 50, 134));
        
    }

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new GatorProtocolDecoder(null));

        verifyAttributes(decoder, binary(
                "242480002600341cad190917022021812497260280594200000000c047010000135400009bb600ff00b90d"));

        verifyAttributes(decoder, binary(
                "2424800026364101b31608041108380273453415301532000000008000010000122800000124000000c40d"));

        verifyNull(decoder, binary(
                "242421000658e3d851150d"));

        verifyAttributes(decoder, binary(
                "242480002658e3d851a60101c662bc00000000000000000000000000470007a30b0c00b10fc900ff00460d"));

        verifyNull(decoder, binary(
                "242421000643e30282070d"));

        verifyPosition(decoder, binary(
                "24248000260009632d141121072702059226180104367500000000c04700079c0c34000ad80b00ff000a0d"),
                position("2014-11-21 07:27:02.000", true, 59.37697, 10.72792));

        verifyPosition(decoder, binary(
                "24248100230CA23285100306145907022346901135294700000000C04001012C0E1100000021CB0D"));

        verifyPosition(decoder, binary(
                "2424800023c2631e00111220104909833268648703804100000000c0470000000b4e00000000550d"),
                position("2011-12-20 10:49:09.000", true, -33.44773, -70.63402));

        verifyPosition(decoder, binary(
                "24248000260009632d141121072702059226180104367500000000c04700079c0c34000ad80b00ff000a0d"));

        verifyPosition(decoder, binary(
                "24247200203c0d07330242313631394143430d2a412304171723280083075207656826e90d"));

        verifyPosition(decoder, binary(
                        "24247200203c0d07330242313631394143430d2a412304171742240083075307656824870d"),
                position("2023-04-17 17:42:24.000", true, 8.51255, 76.94707));

        verifyAttributes(decoder, binary(
                "24247200203c0d07330242313631394143430d2a4123041420015600830758076568329f0d"));

        verifyAttributes(decoder, binary(
                "24248200273c0d0733230417173655008307520765681900000000c0470100000b510000bfce00ff0001720d"));

        verifyPosition(decoder, binary(
                "24248200273c0d0733230417173920008307530765682300000000c0470100000c010000bfd900ff0001730d"));

        verifyPosition(decoder, binary(
                        "24248200273c0d0733230417175405008307520765682000000000c047000000075f0000c02400ff0001ef0d"),
                position("2023-04-17 17:54:05.000", true, 8.51253, 76.94700));
    }

}
