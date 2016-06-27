/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.text.ParseException;

import org.traccar.geofence.GeofenceCircle;
import org.traccar.geofence.GeofenceGeometry;
import org.traccar.geofence.GeofencePolygon;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Geofence extends Extensible {

    public static final String TYPE_GEOFENCE_CILCLE = "geofenceCircle";
    public static final String TYPE_GEOFENCE_POLYGON = "geofencePolygon";

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

    public void setArea(String area) throws ParseException {

        if (area.startsWith("CIRCLE")) {
            geometry = new GeofenceCircle(area);
        } else if (area.startsWith("POLYGON")) {
            geometry = new GeofencePolygon(area);
        } else {
            throw new ParseException("Unknown geometry type", 0);
        }

        this.area = area;
    }

    private GeofenceGeometry geometry;

    @JsonIgnore
    public GeofenceGeometry getGeometry() {
        return geometry;
    }

    public void setGeometry(GeofenceGeometry geometry) {
        area = geometry.toWkt();
        this.geometry = geometry;
    }

}
