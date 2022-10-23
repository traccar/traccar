package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;

public class IntellitracProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new IntellitracProtocolDecoder(null));

        verifyPosition(decoder, text(
                "359316075744331,20201008181424,12.014662,57.826301,0,76,24,10,997,3,0,0.000,4.208,20201008181424,0"));

        verifyNull(decoder, text(
                "$OK:TRACKING"));
        
        verifyPosition(decoder, text(
                "101000001,20100304075545,121.64547,25.06200,0,0,61,7,2,1,0,0.046,0.000,20100304075546,0"),
                position("2010-03-04 07:55:45.000", true, 25.06200, 121.64547));

        verifyPosition(decoder, text(
                "1010000002,20030217132813,121.646060,25.061725,20,157,133,7,0,11,15,0.096,0.000"));

        verifyPosition(decoder, text(
                "1010000002,20030217132813,121.646060,25.061725,20,157,-133,7,0,11,15,0.096,0.000"));

        verifyPosition(decoder, text(
                "1001070919,20130405084206,37.903730,48.011377,0,0,235,10,2,2,0,20.211,0.153"));
        
        verifyPosition(decoder, text(
                "1010000002,20030217144230,121.646102,25.061398,0,0,139,0,0,0,0,0.093,0.000"));
        
        verifyPosition(decoder, text(
                "1010000004,20050513153524,121.646075,25.063675,0,166,50,6,1,0,0,0.118,0.000"));

        verifyPosition(decoder, text(
                "1010000004,20050513154001,121.646075,25.063675,0,166,55,7,1,0,0,0.096,0.000"));
        
        verifyPosition(decoder, text(
                "1010000002,20030217132813,121.646060,25.061725,20,157,0,7,0,11,15"));
        
        verifyPosition(decoder, text(
                "12345,1010000002,20030217132813,121.646060,25.061725,20,157,0,7,0,11,15"));
        
        verifyPosition(decoder, text(
                "1010000002,20030217144230,121.646102,25.061398,0,0,0,7,2,0,0"));
        
        verifyPosition(decoder, text(
                "$RP:12345,1010000002,20030217144230,121.646102,25.061398,0,0,0,7,2,0,0"));
        
        verifyPosition(decoder, text(
                "1010000001,20030105092129,121.651598,25.052325,0,0,33,0,1,0,0"));
        
        verifyPosition(decoder, text(
                "1010000001,20030105092129,-121.651598,-25.052325,0,0,33,0,1,0,0"));
        
        verifyPosition(decoder, text(
                "1015210962,20131010144712,-77.070037,-12.097935,0,0,77,7,2,2,0,0,139446.8,2095,20131010144712,,0.103,0.000"));
        
        verifyPosition(decoder, text(
                "1003269480,20131126100258,10.32989,49.93836,0,304,217,6,2,0,0,0.000,0.000,20131126100258,0,0,0,-40,0,0,-273,0,0,0,0"));

    }

}
