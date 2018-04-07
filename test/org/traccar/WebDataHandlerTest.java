package org.traccar;

import org.junit.Test;
import org.traccar.model.Position;

import java.text.ParseException;

import static org.junit.Assert.assertEquals;

public class WebDataHandlerTest extends ProtocolTest {

    @Test
    public void testFormatRequest() throws Exception {

        Position p = position("2016-01-01 01:02:03.000", true, 20, 30);

        WebDataHandler handler = new WebDataHandler("http://localhost/?fixTime={fixTime}&gprmc={gprmc}&name={name}", false);

        assertEquals(
                "http://localhost/?fixTime=1451610123000&gprmc=$GPRMC,010203.000,A,2000.0000,N,03000.0000,E,0.00,0.00,010116,,*05&name=test",
                handler.formatRequest(p));

    }

    @Test
    public void testPrepareJsonPayload() throws ParseException {

        Position p = position("2016-01-01 01:02:03.000", true, 20, 30);

        WebDataHandler handler = new WebDataHandler("http://localhost/", true);

        assertEquals(
                "{\"position\":{\"id\":0,\"attributes\":{},\"deviceId\":0,\"type\":null,\"protocol\":null,\"serverTime\":null,\"deviceTime\":1451610123000,\"fixTime\":1451610123000,\"outdated\":false,\"valid\":true,\"latitude\":20.0,\"longitude\":30.0,\"altitude\":0.0,\"speed\":0.0,\"course\":0.0,\"address\":null,\"accuracy\":0.0,\"network\":null},\"device\":{\"id\":1,\"attributes\":{},\"groupId\":0,\"name\":\"test\",\"uniqueId\":\"123456789012345\",\"status\":\"offline\",\"lastUpdate\":null,\"positionId\":0,\"geofenceIds\":null,\"phone\":null,\"model\":null,\"contact\":null,\"category\":null,\"disabled\":false}}",
                handler.prepareJsonPayload(p));

    }

}
