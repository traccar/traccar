package org.traccar.protocol;

import org.junit.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Position;

public class GotopProtocolDecoderTest extends ProtocolTest {

    @Test
    public void testDecode() throws Exception {

        var decoder = inject(new GotopProtocolDecoder(null));

        verifyNull(decoder, text(
                ""));

        verifyAttribute(decoder, text(
                "867688033841044,ALM-B1-1,A,DATE:190622,TIME:144956,LAT:22.7193976N,LON:114.3878200E,Speed:000.1,100-22,03.72"),
                Position.KEY_ALARM, Position.ALARM_GEOFENCE_ENTER);

        verifyAttribute(decoder, text(
                "860264050778076,CMD-KEY,V,DATE:220516,TIME:090224,LAT:48.7592616N,LON:003.4658121W,Speed:000.0,057-21,01.60"),
                Position.KEY_ALARM, Position.ALARM_SOS);

        verifyPosition(decoder, text(
                "860264050778076,CMD-T,A,DATE:220516,TIME:100627,LAT:48.7636978N,LON:003.4652398W,Speed:051.4,077-24,01.00,WIFI:{84-a0-6e-94-42-5e&-47,b2-22-7a-56-5a-2a&-48,b4-b0-24-09-91-d4&-76,18-3c-b7-1f-da-67&-83,08-86-3b-94-78-44&-85,40-24-b2-c5-0f-5e&-87,b0-e5-ed-4e-06-61&-87,60-1d-9d-a6-d2-5d&-88,14-eb-b6-a5-55-8c&-88,40-18-b1-d7-28-54&-88},BLE:{1f-cf-4d-3c-61-bf&-91,39-82-d0-ba-34-69&-57,df-3a-31-ac-ad-72&-73,7c-d9-f4-11-0b-a0&-78,03-b5-9f-45-bd-0d&-88}"));

        verifyNull(decoder, text(
                "353327020412763,CMD-X"));

        verifyPosition(decoder, text(
                "867688038677542,ALM-D1,V,DATE:190709,TIME:185214,LAT:50.0422838N,LON:014.4504646E,Speed:004.3,081-20,05.68"));

        verifyPosition(decoder, text(
                "013226009991924,CMD-T,A,DATE:130802,TIME:153721,LAT:25.9757433S,LOT:028.1087816E,Speed:000.0,X-X-X-X-81-26,000,65501-00A0-4B8E"));

        verifyPosition(decoder, text(
                "353327020115804,CMD-T,A,DATE:090329,TIME:223252,LAT:22.7634066N,LOT:114.3964783E,Speed:000.0,84-20,000"),
                position("2009-03-29 22:32:52.000", true, 22.76341, 114.39648));

        verifyPosition(decoder, text(
                "353327020115804,CMD-T,A,DATE:090329,TIME:223252,LAT:22.7634066N,LOT:114.3964783E,Speed:000.0,1-1-0-84-20,000"));
        
        verifyPosition(decoder, text(
                "353327020412763,CMD-F,V,DATE:140125,TIME:183636,LAT:51.6384466N,LOT:000.2863866E,Speed:000.0,61-19,"));

        verifyPosition(decoder, text(
                "013949008891817,CMD-F,A,DATE:150225,TIME:175441,LAT:50.000000N,LOT:008.000000E,Speed:085.9,0-0-0-0-52-31,000,26201-1073-1DF5"));

    }

}
