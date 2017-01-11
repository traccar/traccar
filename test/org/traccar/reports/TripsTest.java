package org.traccar.reports;

import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.model.Position;
import org.traccar.reports.model.TripReport;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class TripsTest extends BaseTest {

    private Date date(String time) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.parse(time);
    }

    private Position position(String time, double speed, double totalDistance) throws ParseException {

        Position position = new Position();

        if (time != null) {
            position.setTime(date(time));
        }
        position.setValid(true);
        position.setSpeed(speed);
        position.set(Position.KEY_TOTAL_DISTANCE, totalDistance);

        return position;
    }

    @Test
    public void testDetectTripsSimple() throws ParseException {

        Collection<Position> data = Arrays.asList(
                position("2016-01-01 00:00:00.000", 0, 0),
                position("2016-01-01 00:01:00.000", 0, 0),
                position("2016-01-01 00:02:00.000", 10, 0),
                position("2016-01-01 00:03:00.000", 10, 1000),
                position("2016-01-01 00:04:00.000", 10, 2000),
                position("2016-01-01 00:05:00.000", 0, 3000),
                position("2016-01-01 00:06:00.000", 0, 3000));

        Collection<TripReport> result = Trips.detectTrips(0.01, 500, 300000, 300000, false, false, data);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        TripReport item = result.iterator().next();

        assertEquals(date("2016-01-01 00:02:00.000"), item.getStartTime());
        assertEquals(date("2016-01-01 00:05:00.000"), item.getEndTime());
        assertEquals(180000, item.getDuration());
        assertEquals(10, item.getAverageSpeed(), 0.01);
        assertEquals(10, item.getMaxSpeed(), 0.01);
        assertEquals(3000, item.getDistance(), 0.01);

    }

}
