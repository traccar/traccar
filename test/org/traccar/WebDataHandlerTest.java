package org.traccar;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.model.Position;

public class WebDataHandlerTest extends ProtocolTest {

    @Test
    public void testFormatRequest() throws Exception {

        Position p = position("2016-01-01 01:02:03.000", true, 20, 30);

        WebDataHandler handler = new WebDataHandler("http://localhost/?fixTime={fixTime}&gprmc={gprmc}&name={name}");

        Assert.assertEquals(
                "http://localhost/?fixTime=1451610123000&gprmc=$GPRMC,010203.000,A,2000.0000,N,03000.0000,E,0.00,0.00,010116,,*05&name=test",
                handler.formatRequest(p));

    }

}
