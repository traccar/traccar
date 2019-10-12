package org.traccar.directions.matrix;

import java.util.ArrayList;
import java.util.List;

public interface Matrix {

    MatrixResponse getMatrix(List<List<Double>> sourceCoord, ArrayList<Double> destCoord);

}
