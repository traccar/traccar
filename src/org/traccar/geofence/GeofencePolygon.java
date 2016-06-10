package org.traccar.geofence;

import java.text.ParseException;
import java.util.ArrayList;

public class GeofencePolygon extends GeofenceGeometry {

    public GeofencePolygon() {
        super();
    }

    public GeofencePolygon(String wkt) throws ParseException {
        super();
        fromWKT(wkt);
    }

    private static class Coordinate {

        public static final double DEGREE360 = 360;

        private double lat;
        private double lon;

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLon() {
            return lon;
        }

        // Need not to confuse algorithm by the abrupt reset of longitude
        public double getLon360() {
            return lon + DEGREE360;
        }

        public void setLon(double lon) {
            this.lon = lon;
        }
    }

    private ArrayList<Coordinate> coordinates;

    private double[] constant;
    private double[] multiple;

    private void precalc() {
        if (coordinates == null) {
            return;
        }
        int polyCorners = coordinates.size();
        int i;
        int j = polyCorners - 1;

        if (constant != null) {
            constant = null;
        }
        if (multiple != null) {
            multiple = null;
        }

        constant = new double[polyCorners];
        multiple = new double[polyCorners];


        for (i = 0; i < polyCorners; j = i++) {
            if (coordinates.get(j).getLon360() == coordinates.get(i).getLon360()) {
                constant[i] = coordinates.get(i).getLat();
                multiple[i] = 0;
            } else {
                constant[i] = coordinates.get(i).getLat()
                        - (coordinates.get(i).getLon360() * coordinates.get(j).getLat())
                        / (coordinates.get(j).getLon360() - coordinates.get(i).getLon360())
                        + (coordinates.get(i).getLon360() * coordinates.get(i).getLat())
                        / (coordinates.get(j).getLon360() - coordinates.get(i).getLon360());
                multiple[i] = (coordinates.get(j).getLat() - coordinates.get(i).getLat())
                        / (coordinates.get(j).getLon360() - coordinates.get(i).getLon360());
            }
        }
    }

    @Override
    public boolean containsPoint(double latitude, double longitude) {

        int polyCorners = coordinates.size();
        int i;
        int j = polyCorners - 1;
        double longitude360 = longitude + Coordinate.DEGREE360;
        boolean oddNodes = false;

        for (i = 0; i < polyCorners; j = i++) {
            if (coordinates.get(i).getLon360() < longitude360
                && coordinates.get(j).getLon360() >= longitude360
                || coordinates.get(j).getLon360() < longitude360
                && coordinates.get(i).getLon360() >= longitude360) {
                oddNodes ^= longitude360 * multiple[i] + constant[i] < latitude;
            }
        }
        return oddNodes;
    }

    @Override
    public String toWKT() {
        StringBuffer buf = new StringBuffer();
        buf.append("POLYGON (");
        for (Coordinate coordinate : coordinates) {
            buf.append(String.valueOf(coordinate.getLat()));
            buf.append(" ");
            buf.append(String.valueOf(coordinate.getLon()));
            buf.append(", ");
        }
        return buf.substring(0, buf.length() - 2) + ")";
    }

    @Override
    public void fromWKT(String wkt) throws ParseException {
        if (coordinates == null) {
            coordinates = new ArrayList<Coordinate>();
        } else {
            coordinates.clear();
        }

        if (!wkt.startsWith("POLYGON")) {
            throw new ParseException("Mismatch geometry type", 0);
        }
        String content = wkt.substring(wkt.indexOf("(") + 1, wkt.indexOf(")"));
        if (content == null || content.equals("")) {
            throw new ParseException("No content", 0);
        }
        String[] commatokens = content.split(",");
        if (commatokens.length < 3) {
            throw new ParseException("Not valid content", 0);
        }

        for (String commatoken : commatokens) {
            String[] tokens = commatoken.trim().split("\\s");
            if (tokens.length != 2) {
                throw new ParseException("Here must be two coordinates: " + commatoken, 0);
            }
            Coordinate coordinate = new Coordinate();
            try {
                coordinate.setLat(Double.parseDouble(tokens[0]));
            } catch (NumberFormatException e) {
                throw new ParseException(tokens[0] + " is not a double", 0);
            }
            try {
                coordinate.setLon(Double.parseDouble(tokens[1]));
            } catch (NumberFormatException e) {
                throw new ParseException(tokens[1] + " is not a double", 0);
            }
            coordinates.add(coordinate);
        }
        precalc();
    }

}
