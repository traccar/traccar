package org.traccar.reports;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.model.Position;

public class ReportUtilsTest {

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
        Assert.assertEquals(ReportUtils.calculateFuel(startPosition, endPosition), "-");
        startPosition.setProtocol("meitrack");
        startPosition.set(Position.KEY_FUEL, 0.07);
        endPosition.set(Position.KEY_FUEL, 0.05);
        Assert.assertEquals(ReportUtils.calculateFuel(startPosition, endPosition), "0.02 %");
        startPosition.setProtocol("galileo");
        Assert.assertEquals(ReportUtils.calculateFuel(startPosition, endPosition), "0.02 %");
        startPosition.setProtocol("noran");
        Assert.assertEquals(ReportUtils.calculateFuel(startPosition, endPosition), "0.02 %");
    }

}
