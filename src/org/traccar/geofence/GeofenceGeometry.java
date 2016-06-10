package org.traccar.geofence;

import java.text.ParseException;

public abstract class GeofenceGeometry {

    public abstract boolean containsPoint(double latitude, double longitude);

    public abstract String toWKT();

    public abstract void fromWKT(String wkt) throws ParseException;

}
