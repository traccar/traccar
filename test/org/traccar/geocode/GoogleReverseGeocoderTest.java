package org.traccar.geocode;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class GoogleReverseGeocoderTest {

    @Test
    public void testGetAddress() {

        ReverseGeocoder reverseGeocoder = new GoogleReverseGeocoder();

        assertEquals(
                "Novotsentral'naya ulitsa, Saint Petersburg, Russia",
                reverseGeocoder.getAddress(60.0, 30.0));

    }

}
