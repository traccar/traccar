/*
 * Copyright 2013 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.traccar.helper.Hashing;
import org.traccar.web.JsonIgnore;

public class User implements Factory {

    @Override
    public User create() {
        return new User();
    }

    private long id;
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    private String email;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    private boolean readonly;
    
    private boolean admin;
    public boolean getAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
    
    private String map;
    public String getMap() { return map; }
    public void setMap(String map) { this.map = map; }

    private String language;
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    private String distanceUnit;
    public String getDistanceUnit() { return distanceUnit; }
    public void setDistanceUnit(String distanceUnit) { this.distanceUnit = distanceUnit; }

    private String speedUnit;
    public String getSpeedUnit() { return speedUnit; }
    public void setSpeedUnit(String speedUnit) { this.speedUnit = speedUnit; }

    private double latitude;
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    private double longitude;
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    private int zoom;
    public int getZoom() { return zoom; }
    public void setZoom(int zoom) { this.zoom = zoom; }

    private String password;
    public String getPassword() { return password; }
    public void setPassword(String password) {
        this.password = password;
        if (password != null && !password.isEmpty()) {
            Hashing.HashingResult hashingResult = Hashing.createHash(password);
            hashedPassword = hashingResult.hash;
            salt = hashingResult.salt;
        }
    }

    private String hashedPassword;
    @JsonIgnore
    public String getHashedPassword() { return hashedPassword; }
    public void setHashedPassword(String hashedPassword) { this.hashedPassword = hashedPassword; }

    private String salt;
    @JsonIgnore
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public boolean isPasswordValid(String password) {
        return Hashing.validatePassword(password, hashedPassword, salt);
    }

}
