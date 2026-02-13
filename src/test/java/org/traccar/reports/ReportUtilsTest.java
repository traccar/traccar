package org.traccar.reports;

import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traccar.BaseTest;
import org.traccar.api.security.PermissionsService;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.reports.common.ReportUtils;
import org.traccar.reports.model.StopReportItem;
import org.traccar.reports.model.TripReportItem;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportUtilsTest extends BaseTest {

    private Storage storage;

    @BeforeEach
    public void init() throws StorageException {
        storage = mock(Storage.class);
        when(storage.getObject(eq(Device.class), any())).thenReturn(mock(Device.class));
    }

    private Date date(String time) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.parse(time);
    }

    private Position position(long id, String time, double speed, double totalDistance) throws ParseException {

        Position position = new Position();

        position.setId(id);
        position.setTime(date(time));
        position.setValid(true);
        position.setSpeed(speed);
        position.set(Position.KEY_MOTION, speed > 0);
        position.set(Position.KEY_TOTAL_DISTANCE, totalDistance);

        return position;
    }

    private Device mockDevice(
            double minimalTripDistance, long minimalTripDuration, long minimalParkingDuration,
            long minimalNoDataDuration, boolean useIgnition) {
        Device device = mock(Device.class);
        when(device.getAttributes()).thenReturn(Map.of(
                Keys.REPORT_TRIP_MINIMAL_TRIP_DISTANCE.getKey(), minimalTripDistance,
                Keys.REPORT_TRIP_MINIMAL_TRIP_DURATION.getKey(), minimalTripDuration,
                Keys.REPORT_TRIP_MINIMAL_PARKING_DURATION.getKey(), minimalParkingDuration,
                Keys.REPORT_TRIP_MINIMAL_NO_DATA_DURATION.getKey(), minimalNoDataDuration,
                Keys.REPORT_TRIP_USE_IGNITION.getKey(), useIgnition));
        return device;
    }

    @Test
    public void testCalculateDistance() {
        Position startPosition = new Position();
        startPosition.set(Position.KEY_TOTAL_DISTANCE, 500.0);
        Position endPosition = new Position();
        endPosition.set(Position.KEY_TOTAL_DISTANCE, 700.0);
        assertEquals(200.0, PositionUtil.calculateDistance(startPosition, endPosition, true), 10);
        startPosition.set(Position.KEY_ODOMETER, 50000);
        endPosition.set(Position.KEY_ODOMETER, 51000);
        assertEquals(1000.0, PositionUtil.calculateDistance(startPosition, endPosition, true), 10);
    }

    @Test
    public void testCalculateSpentFuelWithNoFuelData() {
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);
        Device device = mock(Device.class);
        Position startPosition = new Position();
        Position endPosition = new Position();

        assertEquals(0.0, reportUtils.calculateFuel(startPosition, endPosition, device), 0.01);
    }

    @Test
    public void testCalculateSpentFuelWithFuel() {
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);
        Device device = mock(Device.class);
        Position startPosition = new Position();
        Position endPosition = new Position();

        startPosition.set(Position.KEY_FUEL, 0.7);
        endPosition.set(Position.KEY_FUEL, 0.5);
        assertEquals(0.2, reportUtils.calculateFuel(startPosition, endPosition, device), 0.01);
    }

    @Test
    public void testCalculateSpentFuelWithFuelUsed() {
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);
        Device device = mock(Device.class);
        Position startPosition = new Position();
        Position endPosition = new Position();

        startPosition.set(Position.KEY_FUEL_USED, 10.0);
        endPosition.set(Position.KEY_FUEL_USED, 15.0);
        assertEquals(5.0, reportUtils.calculateFuel(startPosition, endPosition, device), 0.01);
    }

    @Test
    public void testCalculateSpentFuelWithFuelLevel() {
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);
        Device deviceWithCapacity = mock(Device.class);
        when(deviceWithCapacity.hasAttribute(Keys.FUEL_CAPACITY.getKey())).thenReturn(true);
        when(deviceWithCapacity.getDouble(Keys.FUEL_CAPACITY.getKey())).thenReturn(100.0);

        Position startPosition = new Position();
        Position endPosition = new Position();

        startPosition.set(Position.KEY_FUEL_LEVEL, 80.0);
        endPosition.set(Position.KEY_FUEL_LEVEL, 60.0);
        assertEquals(20.0, reportUtils.calculateFuel(startPosition, endPosition, deviceWithCapacity), 0.01);
    }

    @Test
    public void testDetectTripsSimple() throws Exception {

        Stream<Position> data = Stream.of(
                position(1, "2016-01-01 00:00:00.000", 0, 0),
                position(2, "2016-01-01 00:01:00.000", 0, 0),
                position(3, "2016-01-01 00:02:00.000", 10, 0),
                position(4, "2016-01-01 00:03:00.000", 10, 1000),
                position(5, "2016-01-01 00:04:00.000", 10, 2000),
                position(6, "2016-01-01 00:05:00.000", 0, 3000),
                position(7, "2016-01-01 00:15:00.000", 0, 3000),
                position(8, "2016-01-01 00:25:00.000", 0, 3000));
        when(storage.getObjectsStream(eq(Position.class), any())).thenReturn(data);

        Device device = mockDevice(500, 300, 180, 900, false);
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);

        var trips = reportUtils.slowTripsAndStops(device, new Date(), new Date(), TripReportItem.class);

        assertNotNull(trips);
        assertFalse(trips.isEmpty());

        TripReportItem itemTrip = trips.iterator().next();

        assertEquals(date("2016-01-01 00:02:00.000"), itemTrip.getStartTime());
        assertEquals(date("2016-01-01 00:05:00.000"), itemTrip.getEndTime());
        assertEquals(180000, itemTrip.getDuration());
        assertEquals(32.4, itemTrip.getAverageSpeed(), 0.01);
        assertEquals(10, itemTrip.getMaxSpeed(), 0.01);
        assertEquals(3000, itemTrip.getDistance(), 0.01);
    }

    @Test
    public void testDetectStopsSimple() throws Exception {
        Stream<Position> data = Stream.of(
                position(1, "2016-01-01 00:00:00.000", 0, 0),
                position(2, "2016-01-01 00:01:00.000", 0, 0),
                position(3, "2016-01-01 00:02:00.000", 10, 0),
                position(4, "2016-01-01 00:03:00.000", 10, 1000),
                position(5, "2016-01-01 00:04:00.000", 10, 2000),
                position(6, "2016-01-01 00:05:00.000", 0, 3000),
                position(7, "2016-01-01 00:15:00.000", 0, 3000),
                position(8, "2016-01-01 00:25:00.000", 0, 3000));
        when(storage.getObjectsStream(eq(Position.class), any())).thenReturn(data);

        Device device = mockDevice(500, 300, 180, 900, false);
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);
        var stops = reportUtils.slowTripsAndStops(device, new Date(), new Date(), StopReportItem.class);

        assertNotNull(stops);
        assertFalse(stops.isEmpty());

        Iterator<StopReportItem> iterator = stops.iterator();

        StopReportItem itemStop = iterator.next();

        assertEquals(date("2016-01-01 00:00:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:02:00.000"), itemStop.getEndTime());
        assertEquals(120000, itemStop.getDuration());

        itemStop = iterator.next();

        assertEquals(date("2016-01-01 00:05:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:25:00.000"), itemStop.getEndTime());
        assertEquals(1200000, itemStop.getDuration());

    }

    @Test
    public void testDetectTripsSimpleWithIgnition() throws Exception {

        List<Position> data = Arrays.asList(
                position(1, "2016-01-01 00:00:00.000", 0, 0),
                position(2, "2016-01-01 00:01:00.000", 0, 0),
                position(3, "2016-01-01 00:02:00.000", 10, 0),
                position(4, "2016-01-01 00:03:00.000", 10, 1000),
                position(5, "2016-01-01 00:04:00.000", 10, 2000),
                position(6, "2016-01-01 00:05:00.000", 0, 3000),
                position(7, "2016-01-01 00:15:00.000", 0, 3000),
                position(8, "2016-01-01 00:25:00.000", 0, 3000));
        when(storage.getObjectsStream(eq(Position.class), any())).thenReturn(data.stream());

        data.get(5).set(Position.KEY_IGNITION, false);

        Device device = mockDevice(500, 300, 180, 900, true);
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);

        var trips = reportUtils.slowTripsAndStops(device, new Date(), new Date(), TripReportItem.class);

        assertNotNull(trips);
        assertFalse(trips.isEmpty());

        TripReportItem itemTrip = trips.iterator().next();

        assertEquals(date("2016-01-01 00:02:00.000"), itemTrip.getStartTime());
        assertEquals(date("2016-01-01 00:05:00.000"), itemTrip.getEndTime());
        assertEquals(180000, itemTrip.getDuration());
        assertEquals(32.4, itemTrip.getAverageSpeed(), 0.01);
        assertEquals(10, itemTrip.getMaxSpeed(), 0.01);
        assertEquals(3000, itemTrip.getDistance(), 0.01);
    }

    @Test
    public void testDetectStopsSimpleWithIgnition() throws Exception {
        List<Position> data = Arrays.asList(
                position(1, "2016-01-01 00:00:00.000", 0, 0),
                position(2, "2016-01-01 00:01:00.000", 0, 0),
                position(3, "2016-01-01 00:02:00.000", 10, 0),
                position(4, "2016-01-01 00:03:00.000", 10, 1000),
                position(5, "2016-01-01 00:04:00.000", 10, 2000),
                position(6, "2016-01-01 00:05:00.000", 0, 3000),
                position(7, "2016-01-01 00:15:00.000", 0, 3000),
                position(8, "2016-01-01 00:25:00.000", 0, 3000));
        when(storage.getObjectsStream(eq(Position.class), any())).thenReturn(data.stream());

        data.get(5).set(Position.KEY_IGNITION, false);
        Device device = mockDevice(500, 300, 180, 900, true);
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);

        var stops = reportUtils.slowTripsAndStops(device, new Date(), new Date(), StopReportItem.class);

        assertNotNull(stops);
        assertFalse(stops.isEmpty());

        Iterator<StopReportItem> iterator = stops.iterator();

        StopReportItem itemStop = iterator.next();

        assertEquals(date("2016-01-01 00:00:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:02:00.000"), itemStop.getEndTime());
        assertEquals(120000, itemStop.getDuration());

        itemStop = iterator.next();

        assertEquals(date("2016-01-01 00:05:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:25:00.000"), itemStop.getEndTime());
        assertEquals(1200000, itemStop.getDuration());

    }

    @Test
    public void testDetectTripsWithFluctuation() throws Exception {

        Stream<Position> data = Stream.of(
                position(1, "2016-01-01 00:00:00.000", 0, 0),
                position(2, "2016-01-01 00:01:00.000", 0, 0),
                position(3, "2016-01-01 00:02:00.000", 10, 0),
                position(4, "2016-01-01 00:03:00.000", 10, 1000),
                position(5, "2016-01-01 00:04:00.000", 10, 2000),
                position(6, "2016-01-01 00:05:00.000", 10, 3000),
                position(7, "2016-01-01 00:06:00.000", 10, 4000),
                position(8, "2016-01-01 00:07:00.000", 0, 5000),
                position(9, "2016-01-01 00:08:00.000", 10, 6000),
                position(10, "2016-01-01 00:09:00.000", 0, 7000),
                position(11, "2016-01-01 00:19:00.000", 0, 7000),
                position(12, "2016-01-01 00:29:00.000", 0, 7000));
        when(storage.getObjectsStream(eq(Position.class), any())).thenReturn(data);

        Device device = mockDevice(500, 300, 180, 900, false);
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);

        var trips = reportUtils.slowTripsAndStops(device, new Date(), new Date(), TripReportItem.class);

        assertNotNull(trips);
        assertFalse(trips.isEmpty());

        TripReportItem itemTrip = trips.iterator().next();

        assertEquals(date("2016-01-01 00:02:00.000"), itemTrip.getStartTime());
        assertEquals(date("2016-01-01 00:09:00.000"), itemTrip.getEndTime());
        assertEquals(420000, itemTrip.getDuration());
        assertEquals(32.4, itemTrip.getAverageSpeed(), 0.01);
        assertEquals(10, itemTrip.getMaxSpeed(), 0.01);
        assertEquals(7000, itemTrip.getDistance(), 0.01);
    }

    @Test
    public void testDetectStopsWithFluctuation() throws Exception {
        Stream<Position> data = Stream.of(
            position(1, "2016-01-01 00:00:00.000", 0, 0),
            position(2, "2016-01-01 00:01:00.000", 0, 0),
            position(3, "2016-01-01 00:02:00.000", 10, 0),
            position(4, "2016-01-01 00:03:00.000", 10, 1000),
            position(5, "2016-01-01 00:04:00.000", 10, 2000),
            position(6, "2016-01-01 00:05:00.000", 10, 3000),
            position(7, "2016-01-01 00:06:00.000", 10, 4000),
            position(8, "2016-01-01 00:07:00.000", 0, 5000),
            position(9, "2016-01-01 00:08:00.000", 10, 6000),
            position(10, "2016-01-01 00:09:00.000", 0, 7000),
            position(11, "2016-01-01 00:19:00.000", 0, 7000),
            position(12, "2016-01-01 00:29:00.000", 0, 7000));
        when(storage.getObjectsStream(eq(Position.class), any())).thenReturn(data);
        Device device = mockDevice(500, 300, 180, 900, false);
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);

        var stops = reportUtils.slowTripsAndStops(device, new Date(), new Date(), StopReportItem.class);

        assertNotNull(stops);
        assertFalse(stops.isEmpty());

        Iterator<StopReportItem> iterator = stops.iterator();

        StopReportItem itemStop = iterator.next();

        assertEquals(date("2016-01-01 00:00:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:02:00.000"), itemStop.getEndTime());
        assertEquals(120000, itemStop.getDuration());

        itemStop = iterator.next();

        assertEquals(date("2016-01-01 00:09:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:29:00.000"), itemStop.getEndTime());
        assertEquals(1200000, itemStop.getDuration());

    }

    @Test
    public void testDetectStopsOnly() throws Exception {

        var data = Stream.of(
                position(1, "2016-01-01 00:00:00.000", 0, 0),
                position(2, "2016-01-01 00:01:00.000", 0, 0),
                position(3, "2016-01-01 00:02:00.000", 1, 0),
                position(4, "2016-01-01 00:03:00.000", 0, 0),
                position(5, "2016-01-01 00:04:00.000", 1, 0),
                position(6, "2016-01-01 00:05:00.000", 0, 0));
        when(storage.getObjectsStream(eq(Position.class), any())).thenReturn(data);

        Device device = mockDevice(500, 300, 200, 900, false);
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);

        var result = reportUtils.slowTripsAndStops(device, new Date(), new Date(), StopReportItem.class);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        StopReportItem itemStop = result.iterator().next();

        assertEquals(date("2016-01-01 00:00:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:05:00.000"), itemStop.getEndTime());
        assertEquals(300000, itemStop.getDuration());

    }

    @Test
    public void testDetectStopsWithTripCut() throws Exception {

        var data = Stream.of(
                position(1, "2016-01-01 00:00:00.000", 0, 0),
                position(2, "2016-01-01 00:01:00.000", 0, 0),
                position(3, "2016-01-01 00:02:00.000", 0, 0),
                position(4, "2016-01-01 00:03:00.000", 0, 0),
                position(5, "2016-01-01 00:04:00.000", 1, 0),
                position(6, "2016-01-01 00:05:00.000", 2, 0));
        when(storage.getObjectsStream(eq(Position.class), any())).thenReturn(data);

        Device device = mockDevice(500, 300, 200, 900, false);
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);

        var result = reportUtils.slowTripsAndStops(device, new Date(), new Date(), StopReportItem.class);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        StopReportItem itemStop = result.iterator().next();

        assertEquals(date("2016-01-01 00:00:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:05:00.000"), itemStop.getEndTime());
        assertEquals(300000, itemStop.getDuration());

    }

    @Test
    public void testDetectStopsStartedFromTrip() throws Exception {

        var data = Stream.of(
                position(1, "2016-01-01 00:00:00.000", 2, 0),
                position(2, "2016-01-01 00:01:00.000", 1, 0),
                position(3, "2016-01-01 00:02:00.000", 0, 0),
                position(4, "2016-01-01 00:12:00.000", 0, 0),
                position(5, "2016-01-01 00:22:00.000", 0, 0),
                position(6, "2016-01-01 00:32:00.000", 0, 0));
        when(storage.getObjectsStream(eq(Position.class), any())).thenReturn(data);

        Device device = mockDevice(500, 300, 200, 900, false);
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);

        var result = reportUtils.slowTripsAndStops(device, new Date(), new Date(), StopReportItem.class);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        StopReportItem itemStop = result.iterator().next();

        assertEquals(date("2016-01-01 00:02:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:32:00.000"), itemStop.getEndTime());
        assertEquals(1800000, itemStop.getDuration());

    }

    @Test
    public void testDetectStopsMoving() throws Exception {

        var data = Arrays.asList(
                position(1, "2016-01-01 00:00:00.000", 5, 0),
                position(2, "2016-01-01 00:01:00.000", 5, 0),
                position(3, "2016-01-01 00:02:00.000", 3, 0),
                position(4, "2016-01-01 00:03:00.000", 5, 0),
                position(5, "2016-01-01 00:04:00.000", 5, 0),
                position(6, "2016-01-01 00:05:00.000", 5, 0));
        when(storage.getObjects(eq(Position.class), any())).thenReturn(data);

        Device device = mockDevice(500, 300, 200, 900, false);
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);

        var result = reportUtils.slowTripsAndStops(device, new Date(), new Date(), StopReportItem.class);

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test
    public void testDetectTripByGap() throws Exception {

        var data = Stream.of(
                position(1, "2016-01-01 00:00:00.000", 7, 100),
                position(2, "2016-01-01 00:01:00.000", 7, 300),
                position(3, "2016-01-01 00:02:00.000", 5, 500),
                position(4, "2016-01-01 00:03:00.000", 5, 600),
                position(5, "2016-01-01 00:04:00.000", 3, 700),
                position(6, "2016-01-01 00:23:00.000", 2, 700),
                position(7, "2016-01-01 00:24:00.000", 5, 800),
                position(8, "2016-01-01 00:25:00.000", 5, 900));
        when(storage.getObjectsStream(eq(Position.class), any())).thenReturn(data);

        Device device = mockDevice(500, 200, 200, 900, false);
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);

        var trips = reportUtils.slowTripsAndStops(device, new Date(), new Date(), TripReportItem.class);

        assertNotNull(trips);
        assertFalse(trips.isEmpty());

        TripReportItem itemTrip = trips.iterator().next();

        assertEquals(date("2016-01-01 00:00:00.000"), itemTrip.getStartTime());
        assertEquals(date("2016-01-01 00:04:00.000"), itemTrip.getEndTime());
        assertEquals(240000, itemTrip.getDuration());
        assertEquals(4.86, itemTrip.getAverageSpeed(), 0.01);
        assertEquals(7, itemTrip.getMaxSpeed(), 0.01);
        assertEquals(600, itemTrip.getDistance(), 0.01);
    }

    @Test
    public void testDetectStopByGap() throws Exception {
        var data = Stream.of(
                position(1, "2016-01-01 00:00:00.000", 7, 100),
                position(2, "2016-01-01 00:01:00.000", 7, 300),
                position(3, "2016-01-01 00:02:00.000", 5, 500),
                position(4, "2016-01-01 00:03:00.000", 5, 600),
                position(5, "2016-01-01 00:04:00.000", 3, 700),
                position(6, "2016-01-01 00:23:00.000", 2, 700),
                position(7, "2016-01-01 00:24:00.000", 5, 800),
                position(8, "2016-01-01 00:25:00.000", 5, 900));
        when(storage.getObjectsStream(eq(Position.class), any())).thenReturn(data);
        ReportUtils reportUtils = new ReportUtils(
                mock(Config.class), storage, mock(PermissionsService.class), mock(VelocityEngine.class), null);
        Device device = mockDevice(500, 200, 200, 900, false);
        var stops = reportUtils.slowTripsAndStops(device, new Date(), new Date(), StopReportItem.class);

        assertNotNull(stops);
        assertFalse(stops.isEmpty());

        StopReportItem itemStop = stops.iterator().next();

        assertEquals(date("2016-01-01 00:04:00.000"), itemStop.getStartTime());
        assertEquals(date("2016-01-01 00:25:00.000"), itemStop.getEndTime());
        assertEquals(1260000, itemStop.getDuration());
    }

}
