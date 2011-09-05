/*
 * Copyright 2010 Anton Tananaev (anton@tananaev.com)
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
package net.sourceforge.opentracking;

import java.util.Date;

/**
 * Position information
 */
public class Position {

    /**
     * Id
     */
    private Long id;
    
    public Long getId() {
        return id;
    }

    public void setId(Long newId) {
        id = newId;
    }

    /**
     * Device
     */
    private Long deviceId;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long newDeviceId) {
        deviceId = newDeviceId;
    }

    /**
     * Time (UTC)
     */
    private Date time;

    public Date getTime() {
        return time;
    }

    public void setTime(Date newTime) {
        time = newTime;
    }

    /**
     * Validity flag
     */
    private Boolean valid;

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean newValid) {
        valid = newValid;
    }

    /**
     * Latitude
     */
    private Double latitude;

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double newLatitude) {
        latitude = newLatitude;
    }

    /**
     * Longitude
     */
    private Double longitude;

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double newLongitude) {
        longitude = newLongitude;
    }
    
    /**
     * Altitude
     */
    private Double altitude;
    
    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double newAltitude) {
        altitude = newAltitude;
    }

    /**
     * Speed (knots)
     */
    private Double speed;

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double newSpeed) {
        speed = newSpeed;
    }

    /**
     * Course
     */
    private Double course;

    public Double getCourse() {
        return course;
    }

    public void setCourse(Double newCourse) {
        course = newCourse;
    }

}
