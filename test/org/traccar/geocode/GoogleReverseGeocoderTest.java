package org.traccar.geocode;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class GoogleReverseGeocoderTest {

    @Test
    public void testGetAddress() {

        ReverseGeocoder reverseGeocoder = new GoogleReverseGeocoder();
        
        assertEquals(
                "ulitsa Morskiye dubki, 2, Lisy Nos, Saint Petersburg, Russia, 197755",
                reverseGeocoder.getAddress(60.0, 30.0));

    }

}
