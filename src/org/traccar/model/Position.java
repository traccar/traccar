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

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.traccar.database.Convertable;
import org.traccar.database.ObjectConverter;

public class Position implements Convertable {
    
    private long id;
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    private String protocol;
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    private long deviceId;
    public long getDeviceId() { return deviceId; }
    public void setDeviceId(long deviceId) { this.deviceId = deviceId; }

    private Date serverTime;
    public Date getServerTime() { return serverTime; }
    public void setServerTime(Date serverTime) { this.serverTime = serverTime; }

    private Date deviceTime;
    public Date getDeviceTime() { return deviceTime; }
    public void setDeviceTime(Date deviceTime) { this.deviceTime = deviceTime; }

    private Date fixTime;
    public Date getFixTime() { return fixTime; }
    public void setFixTime(Date fixTime) { this.fixTime = fixTime; }
    
    public void setTime(Date time) {
        deviceTime = time;
        fixTime = time;
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

    private final Map<String, Object> other = new LinkedHashMap<String, Object>();
    public void set(String key, Object value) {
        if (value != null && (!(value instanceof String) || !((String) value).isEmpty())) {
            other.put(key, value);
        }
    }
    public String getOther() {
        return MiscFormatter.toXmlString(other);
    }

    @Override
    public JsonObject toJson() {
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("id", id);
        json.add("protocol", protocol);
        json.add("deviceId", deviceId);
        json.add("serverTime", ObjectConverter.dateFormat.format(serverTime));
        json.add("deviceTime", ObjectConverter.dateFormat.format(deviceTime));
        json.add("fixTime", ObjectConverter.dateFormat.format(fixTime));
        json.add("valid", valid);
        json.add("latitude", latitude);
        json.add("longitude", longitude);
        json.add("altitude", altitude);
        json.add("speed", speed);
        json.add("course", course);
        json.add("address", address);
        //json.add("extendedInfo", extendedInfo);
        return json.build();
    }

    @Override
    public void fromJson(JsonObject json) throws ParseException {
        id = json.getJsonNumber("id").longValue();
        protocol = json.getString("protocol");
        deviceId = json.getJsonNumber("deviceId").longValue();
        serverTime = ObjectConverter.dateFormat.parse(json.getString("serverTime"));
        deviceTime = ObjectConverter.dateFormat.parse(json.getString("deviceTime"));
        fixTime = ObjectConverter.dateFormat.parse(json.getString("fixTime"));
        valid = json.getBoolean("valid");
        latitude = json.getJsonNumber("latitude").doubleValue();
        longitude = json.getJsonNumber("longitude").doubleValue();
        altitude = json.getJsonNumber("altitude").doubleValue();
        speed = json.getJsonNumber("speed").doubleValue();
        course = json.getJsonNumber("course").doubleValue();
        address = json.getString("address");
        //extendedInfo = json.getString("extendedInfo");
    }

}
