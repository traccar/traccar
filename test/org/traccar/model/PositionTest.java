package org.traccar.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class PositionTest {
	
    @Test
    public void positionDistanceIsZero() {
    	Position firstPosition = new Position();
    	firstPosition.setLatitude(Double.valueOf(3));
    	firstPosition.setLongitude(Double.valueOf(3));

    	Position secondPosition = new Position();
    	secondPosition.setLatitude(Double.valueOf(3));
    	secondPosition.setLongitude(Double.valueOf(3));
    	
    	assertTrue(firstPosition.getDistanceInMetersFromPosition(secondPosition).compareTo(Double.valueOf(0)) == 0);
    }
    
    @Test
    public void positionDistanceInOneLongitude() {
    	Position firstPosition = new Position();
    	firstPosition.setLatitude(Double.valueOf(0));
    	firstPosition.setLongitude(Double.valueOf(0));

    	Position secondPosition = new Position();
    	secondPosition.setLatitude(Double.valueOf(0));
    	secondPosition.setLongitude(Double.valueOf(1));
    	
    	// testing for 1 meter precision
    	
    	double calculatedDistance = firstPosition.getDistanceInMetersFromPosition(secondPosition).doubleValue();
    	double expectedDistance = 111194.9;
    	double error = calculatedDistance - expectedDistance;
    	
    	assertTrue(error < 0.5 && error > -0.5);
    }
    
    @Test
    public void positionDistance() {
    	Position firstPosition = new Position();
    	firstPosition.setLatitude(Double.valueOf(15.73));
    	firstPosition.setLongitude(Double.valueOf(-100.62));

    	Position secondPosition = new Position();
    	secondPosition.setLatitude(Double.valueOf(-58));
    	secondPosition.setLongitude(Double.valueOf(24.98));
    	
    	// testing for 1 meter precision
    	
    	double calculatedDistance = firstPosition.getDistanceInMetersFromPosition(secondPosition).doubleValue();
    	double expectedDistance = 13542638.6;
    	double error = calculatedDistance - expectedDistance;
    	
    	assertTrue(error < 0.5 && error > -0.5);    }
    
    

}
