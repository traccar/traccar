package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;

public class LaipacProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new LaipacProtocolDecoder(null));

        verifyPosition(decoder, text(
                "$AVRMC,80006405,212645,r,3013.9938,N,08133.3998,W,0.00,0.00,010317,a,4076,0,1,0,0,53170583,310260*78"));

        verifyNull(decoder, text(
                "$AVSYS,99999999,V1.50,SN0000103,32768*15"));

        verifyNull(decoder, text(
                "$ECHK,99999999,0*35"));

        verifyNull(decoder, text(
                "$AVSYS,MSG00002,14406,7046811160,64*1A"));

        verifyAttributes(decoder, text(
                "$EAVSYS,MSG00002,8931086013104404999,,Owner,0x52014406*76"));

        verifyNull(decoder, text(
                "$ECHK,MSG00002,0*5E"));

        verifyPosition(decoder, text(
                "$AVRMC,99999999,164339,A,4351.0542,N,07923.5445,W,0.29,78.66,180703,0,3.727,17,1,0,0*37"),
                position("2003-07-18 16:43:39.000", true, 43.85090, -79.39241));

        verifyPosition(decoder, text(
                "$AVRMC,99999999,164339,a,4351.0542,N,07923.5445,W,0.29,78.66,180703,0,3.727,17,1,0,0*17"));

        verifyPosition(decoder, text(
                "$AVRMC,99999999,164339,v,4351.0542,N,07923.5445,W,0.29,78.66,180703,0,3.727,17,1,0,0*00"));

        verifyPosition(decoder, text(
                "$AVRMC,99999999,164339,r,4351.0542,N,07923.5445,W,0.29,78.66,180703,0,3.727,17,1,0,0*04"));

        verifyPosition(decoder, text(
                "$AVRMC,99999999,164339,A,4351.0542,N,07923.5445,W,0.29,78.66,180703,S,3.727,17,1,0,0*54"));

        verifyPosition(decoder, text(
                "$AVRMC,99999999,164339,A,4351.0542,N,07923.5445,W,0.29,78.66,180703,T,3.727,17,1,0,0*53"));

        verifyPosition(decoder, text(
                "$AVRMC,99999999,164339,A,4351.0542,N,07923.5445,W,0.29,78.66,180703,3,3.727,17,1,0,0*34"));

        verifyPosition(decoder, text(
                "$AVRMC,99999999,164339,A,4351.0542,N,07923.5445,W,0.29,78.66,180703,X,3.727,17,1,0,0*5F"));

        verifyPosition(decoder, text(
                "$AVRMC,99999999,164339,A,4351.0542,N,07923.5445,W,0.29,78.66,180703,4,3.727,17,1,0,0*33"));

        verifyPosition(decoder, text(
                "$AVRMC,MSG00002,003016,v,0000.0000,N,00000.0000,E,0.00,0.00,200614,0,3804,167,1,0,0,0D7AB913,020408*23"));

        verifyPosition(decoder, text(
                "$AVRMC,MSG00002,003049,V,0000.0000,N,00000.0000,E,0.00,0.00,200614,H,3804,167,1,0,0,0D7AB913,020408*71"));

        verifyPosition(decoder, text(
                "$AVRMC,MSG00002,041942,V,0000.0000,N,00000.0000,E,0.00,0.00,200614,H,4115,167,1,0,0*0E"));

        verifyPosition(decoder, text(
                "$AVRMC,MSG00002,043703,V,0000.0000,N,00000.0000,E,0.00,0.00,200614,H,4115,167,1,0,0*07"));

        verifyPosition(decoder, text(
                "$AVRMC,MSG00002,043750,V,0000.0000,N,00000.0000,E,0.00,0.00,200614,H,4115,167,1,0,0*01"));

        verifyPosition(decoder, text(
                "$AVRMC,MSG00002,124022,V,0000.0000,N,00000.0000,E,0.00,0.00,240614,3,4076,167,1,0,0,0D7AB913,020408*0D"));

        verifyPosition(decoder, text(
                "$AVRMC,MSG00002,124058,A,5053.0447,N,00557.8549,E,0.45,65.06,240614,0,4037,167,1,0,0,0D7AB913,020408*26"));

        verifyPosition(decoder, text(
                "$AVRMC,MSG00002,124144,A,5053.0450,N,00557.8544,E,0.00,65.06,240614,3,4076,167,1,0,0,0D7AB913,020408*26"));

        verifyPosition(decoder, text(
                "$AVRMC,MSG00002,125142,R,5053.0442,N,00557.8694,E,1.21,40.90,240614,0,4037,167,1,0,0,0D7AB913,020408*33"));

        verifyPosition(decoder, text(
                "$AVRMC,MSG00002,125517,R,5053.0442,N,00557.8694,E,0.00,0.00,240614,H,4076,167,1,0,0,0D7AB913,020408*75"));

        verifyPosition(decoder, text(
                "$AVRMC,MSG00002,043104,p,5114.4664,N,00534.3308,E,0.00,0.00,280614,0,4115,495,1,0,0,0D48C3DC,020408*52"));

        verifyPosition(decoder, text(
                "$AVRMC,MSG00002,050601,P,5114.4751,N,00534.3175,E,0.00,0.00,280614,0,4115,495,1,0,0,0D48C3DC,020408*7D"));

        verifyPosition(decoder, text(
                "$AVRMC,96414215,170046,p,4310.7965,N,07652.0816,E,0.00,0.00,071016,0,4069,98,1,0,0*04"));

        verifyPosition(decoder, text(
                "$AVRMC,999999999999999,111602,r,5050.1262,N,00419.9660,E,0.00,0.00,120318,0,3843,95,1,0,0,3EE4A617,020610*47"));

        verifyPosition(decoder, text(
                "$AVRMC,358174067149865,143456,R,5050.1285,N,00420.0620,E,0.00,309.27,190318,0,3455,119,1,0,0,3EE4A617,020610*54"));

        verifyPosition(decoder, text(
                "$AVRMC,999999999999999,084514,r,5050.1314,N,00419.9719,E,0.68,306.39,120318,0,3882,84,1,0,0,3EE4A617,020610*4D"));

        // Alarm button
        verifyPosition(decoder, text(
                "$AVRMC,358174067149865,142945,R,5050.1254,N,00420.0490,E,0.00,0.00,190318,3,3455,119,1,0,0,3EE4A617,020610*53"));

        // G-Sensor
        verifyPosition(decoder, text(
                "$AVRMC,358174067149865,143407,R,5050.1254,N,00420.0490,E,0.00,0.00,190318,8,3455,119,1,0,0,3EE4A617,020610*52"));

        // Powered off
        verifyPosition(decoder, text(
                "$AVRMC,358174067149865,143648,A,5050.1141,N,00420.0525,E,1.24,174.38,190318,H,3455,119,1,0,0,3EE4A617,020610*3E"));

        // No network
        verifyPosition(decoder, text(
                "$AVRMC,358174067149865,143747,R,5050.1124,N,00420.0542,E,1.34,161.96,190318,a,3416,119,1,0,0*7D"));

        // Zero LAC, CID, MCC, MNC
        verifyPosition(decoder, text(
                "$AVRMC,358174067149865,143747,P,5050.1124,N,00420.0542,E,1.34,161.96,190318,A,3416,119,1,0,0,0,0*5F"));

        // New unknown parameters
        verifyPosition(decoder, text(
                "$AVRMC,358174067149865,143747,P,5050.1124,N,00420.0542,E,1.34,161.96,190318,A,3416,119,1,0,0,0,0,0,0*5F"));
    }

}
