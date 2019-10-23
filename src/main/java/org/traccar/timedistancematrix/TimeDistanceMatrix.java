package org.traccar.timedistancematrix;

import java.util.ArrayList;
import java.util.List;

public interface TimeDistanceMatrix {

    TimeDistanceResponse getTimeDistanceMatrix(List<List<Double>> sourceLocations,
                                               ArrayList<Double> destinationLocation);

}
