package org.traccar.geocode;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class NominatimReverseGeocoderTest {

    @Test
    public void testGetAddress() {

        ReverseGeocoder reverseGeocoder = new NominatimReverseGeocoder("http://nominatim.openstreetmap.org/reverse");
        assertEquals("Budapest, Alkotás utca 15.", reverseGeocoder.getAddress(47.4981623,19.0236603));
        
        /*assertEquals(
                "ulitsa Morskiye dubki, 2, Lisy Nos, Saint Petersburg, Russia, 197755",
                reverseGeocoder.getAddress(60.0, 30.0));*/

    }

}
