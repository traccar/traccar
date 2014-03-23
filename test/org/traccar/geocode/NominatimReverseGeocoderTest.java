package org.traccar.geocode;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class NominatimReverseGeocoderTest {

    @Test
    public void testGetAddress() {

        ReverseGeocoder reverseGeocoder = new NominatimReverseGeocoder("http://nominatim.openstreetmap.org/reverse");
        
        /*assertEquals(
                "ulitsa Morskiye dubki, 2, Lisy Nos, Saint Petersburg, Russia, 197755",
                reverseGeocoder.getAddress(60.0, 30.0));*/

    }

}
