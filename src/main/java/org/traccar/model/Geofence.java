/*
 * Copyright 2016 - 2025 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.traccar.geofence.GeofenceCircle;
import org.traccar.geofence.GeofenceGeometry;
import org.traccar.geofence.GeofencePolygon;
import org.traccar.geofence.GeofencePolyline;
import org.traccar.storage.QueryIgnore;
import org.traccar.storage.StorageName;

import java.text.ParseException;

@StorageName("tc_geofences")
public class Geofence extends ExtendedModel implements Schedulable {

    private long calendarId;

    @Override
    public long getCalendarId() {
        return calendarId;
    }

    @Override
    public void setCalendarId(long calendarId) {
        this.calendarId = calendarId;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private String area;

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
        geometry = null;
    }

    private GeofenceGeometry geometry;

    @QueryIgnore
    @JsonIgnore
    public GeofenceGeometry getGeometry() {
        if (geometry == null) {
            try {
                if (area.startsWith("CIRCLE")) {
                    geometry = new GeofenceCircle(area);
                } else if (area.startsWith("POLYGON")) {
                    geometry = new GeofencePolygon(area);
                } else if (area.startsWith("LINESTRING")) {
                    geometry = new GeofencePolyline(area, getDouble("polylineDistance", 25.0));
                } else {
                    throw new IllegalArgumentException("Unknown geometry type");
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return geometry;
    }

    @QueryIgnore
    @JsonIgnore
    public void setGeometry(GeofenceGeometry geometry) {
        setArea(geometry.toWkt());
    }

    public boolean containsPosition(Position position) {
        return getGeometry().containsPoint(position.getLatitude(), position.getLongitude());
    }

}
