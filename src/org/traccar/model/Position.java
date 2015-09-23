/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.util.Date;

public class Position extends Event implements Factory {

    @Override
    public Position create() {
        return new Position();
    }

    private Date fixTime;
    public Date getFixTime() { return fixTime; }
    public void setFixTime(Date fixTime) { this.fixTime = fixTime; }
    
    public void setTime(Date time) {
        setDeviceTime(time);
        setFixTime(time);
    }

    private boolean valid;
    public boolean getValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    private double latitude;
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    private double longitude;
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    private double altitude;
    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }

    private double speed; // value in knots
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    private double course;
    public double getCourse() { return course; }
    public void setCourse(double course) { this.course = course; }

    private String address;
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

}
