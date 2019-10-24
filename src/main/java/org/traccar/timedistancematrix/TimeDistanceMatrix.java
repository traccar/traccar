package org.traccar.timedistancematrix;

import java.util.List;

public interface TimeDistanceMatrix {
    TimeDistanceResponse getTimeDistanceMatrix(List<List<Double>> sourceLocations,
            List<Double> destinationLocation);
}
