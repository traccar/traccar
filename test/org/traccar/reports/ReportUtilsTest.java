package org.traccar.reports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.BaseTest;
import org.traccar.model.Position;
import org.traccar.reports.model.BaseReport;
import org.traccar.reports.model.StopReport;
import org.traccar.reports.model.TripReport;
import org.traccar.reports.model.TripsConfig;

public class ReportUtilsTest extends BaseTest {

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
    public void testCalculateDistance() {
        Position startPosition = new Position();
        startPosition.set(Position.KEY_TOTAL_DISTANCE, 500.0);
        Position endPosition = new Position();
        endPosition.set(Position.KEY_TOTAL_DISTANCE, 700.0);
        Assert.assertEquals(ReportUtils.calculateDistance(startPosition, endPosition), 200.0, 10);
        startPosition.set(Position.KEY_ODOMETER, 50000);
        endPosition.set(Position.KEY_ODOMETER, 51000);
        Assert.assertEquals(ReportUtils.calculateDistance(startPosition, endPosition), 1000.0, 10);
    }

    @Test
    public void testCalculateSpentFuel() {
        Position startPosition = new Position();
        Position endPosition = new Position();
        Assert.assertNull(ReportUtils.calculateFuel(startPosition, endPosition));
        startPosition.set(Position.KEY_FUEL_LEVEL, 0.7);
        endPosition.set(Position.KEY_FUEL_LEVEL, 0.5);
        Assert.assertEquals(ReportUtils.calculateFuel(startPosition, endPosition), "0.2");
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
                position("2016-01-01 00:06:00.000", 0, 3000),
                position("2016-01-01 00:07:00.000", 0, 3000));

        TripsConfig tripsConfig = new TripsConfig(500, 300000, 180000, false, 900000);

        Collection<? extends BaseReport> result = ReportUtils.detectTripsAndStops(tripsConfig, false, 0.01, data, true);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        TripReport itemTrip = (TripReport) result.iterator().next();

        assertEquals(date("2016-01-01 00:02:00.000"), itemTrip.getStartTime());
        assertEquals(date("2016-01-01 00:05:00.000"), itemTrip.getEndTime());
        assertEquals(180000, itemTrip.getDuration());
        assertEquals(10, itemTrip.getAverageSpeed(), 0.01);
        assertEquals(10, itemTrip.getMaxSpeed(), 0.01);
        assertEquals(3000, itemTrip.getDistance(), 0.01);

        result = ReportUtils.detectTripsAndStops(tripsConfig, false, 0.01, data, false);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        StopReport itemStop = (StopReport) result.iterator().next();

        assertEquals(date("2016-01-01 00:05:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:07:00.000"), itemStop.getEndTime());
        assertEquals(120000, itemStop.getDuration());

    }

    @Test
    public void testDetectStopsOnly() throws ParseException {

        Collection<Position> data = Arrays.asList(
                position("2016-01-01 00:00:00.000", 0, 0),
                position("2016-01-01 00:01:00.000", 0, 0),
                position("2016-01-01 00:02:00.000", 1, 0),
                position("2016-01-01 00:03:00.000", 0, 0),
                position("2016-01-01 00:04:00.000", 1, 0),
                position("2016-01-01 00:05:00.000", 0, 0));

        TripsConfig tripsConfig = new TripsConfig(500, 300000, 200000, false, 900000);

        Collection<? extends BaseReport> result = ReportUtils.detectTripsAndStops(tripsConfig, false, 0.01, data, false);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        StopReport itemStop = (StopReport) result.iterator().next();

        assertEquals(date("2016-01-01 00:00:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:05:00.000"), itemStop.getEndTime());
        assertEquals(300000, itemStop.getDuration());

    }

    @Test
    public void testDetectStopsWithTripCut() throws ParseException {

        Collection<Position> data = Arrays.asList(
                position("2016-01-01 00:00:00.000", 0, 0),
                position("2016-01-01 00:01:00.000", 0, 0),
                position("2016-01-01 00:02:00.000", 0, 0),
                position("2016-01-01 00:03:00.000", 0, 0),
                position("2016-01-01 00:04:00.000", 1, 0),
                position("2016-01-01 00:05:00.000", 2, 0));

        TripsConfig tripsConfig = new TripsConfig(500, 300000, 200000, false, 900000);

        Collection<? extends BaseReport> result = ReportUtils.detectTripsAndStops(tripsConfig, false, 0.01, data, false);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        StopReport itemStop = (StopReport) result.iterator().next();

        assertEquals(date("2016-01-01 00:00:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:04:00.000"), itemStop.getEndTime());
        assertEquals(240000, itemStop.getDuration());

        tripsConfig.setGreedyParking(true);

        result = ReportUtils.detectTripsAndStops(tripsConfig, false, 0.01, data, false);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        itemStop = (StopReport) result.iterator().next();

        assertEquals(date("2016-01-01 00:00:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:05:00.000"), itemStop.getEndTime());
        assertEquals(300000, itemStop.getDuration());

    }
    
    @Test
    public void testDetectStopsStartedFromTrip() throws ParseException {

        Collection<Position> data = Arrays.asList(
                position("2016-01-01 00:00:00.000", 2, 0),
                position("2016-01-01 00:01:00.000", 1, 0),
                position("2016-01-01 00:02:00.000", 0, 0),
                position("2016-01-01 00:03:00.000", 0, 0),
                position("2016-01-01 00:04:00.000", 0, 0),
                position("2016-01-01 00:05:00.000", 0, 0));

        TripsConfig tripsConfig = new TripsConfig(500, 300000, 200000, false, 900000);

        Collection<? extends BaseReport> result = ReportUtils.detectTripsAndStops(tripsConfig, false, 0.01, data, false);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        StopReport itemStop = (StopReport) result.iterator().next();

        assertEquals(date("2016-01-01 00:00:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:05:00.000"), itemStop.getEndTime());
        assertEquals(300000, itemStop.getDuration());

    }

    @Test
    public void testDetectStopsMoving() throws ParseException {

        Collection<Position> data = Arrays.asList(
                position("2016-01-01 00:00:00.000", 5, 0),
                position("2016-01-01 00:01:00.000", 5, 0),
                position("2016-01-01 00:02:00.000", 3, 0),
                position("2016-01-01 00:03:00.000", 5, 0),
                position("2016-01-01 00:04:00.000", 5, 0),
                position("2016-01-01 00:05:00.000", 5, 0));

        TripsConfig tripsConfig = new TripsConfig(500, 300000, 200000, false, 900000);

        Collection<? extends BaseReport> result = ReportUtils.detectTripsAndStops(tripsConfig, false, 0.01, data, false);

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test
    public void testDetectTripAndStopByGap() throws ParseException {

        Collection<Position> data = Arrays.asList(
                position("2016-01-01 00:00:00.000", 7, 100),
                position("2016-01-01 00:01:00.000", 7, 300),
                position("2016-01-01 00:02:00.000", 5, 500),
                position("2016-01-01 00:03:00.000", 5, 600),
                position("2016-01-01 00:04:00.000", 3, 700),
                position("2016-01-01 00:23:00.000", 2, 700),
                position("2016-01-01 00:24:00.000", 5, 800),
                position("2016-01-01 00:25:00.000", 5, 900));

        TripsConfig tripsConfig = new TripsConfig(500, 200000, 200000, false, 900000);

        Collection<? extends BaseReport> result = ReportUtils.detectTripsAndStops(tripsConfig, false, 0.01, data, true);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        TripReport itemTrip = (TripReport) result.iterator().next();

        assertEquals(date("2016-01-01 00:00:00.000"), itemTrip.getStartTime());
        assertEquals(date("2016-01-01 00:04:00.000"), itemTrip.getEndTime());
        assertEquals(240000, itemTrip.getDuration());
        assertEquals(6.75, itemTrip.getAverageSpeed(), 0.01);
        assertEquals(7, itemTrip.getMaxSpeed(), 0.01);
        assertEquals(600, itemTrip.getDistance(), 0.01);

        result = ReportUtils.detectTripsAndStops(tripsConfig, false, 0.01, data, false);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        StopReport itemStop = (StopReport) result.iterator().next();

        assertEquals(date("2016-01-01 00:04:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:23:00.000"), itemStop.getEndTime());
        assertEquals(1140000, itemStop.getDuration());
    }

}
