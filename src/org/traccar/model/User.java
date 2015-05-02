/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.traccar.database.Convertable;

public class User implements Convertable {

    private long id;
    
    private String email;
    
    private String password;
    
    private boolean readonly;
    
    private boolean admin;
    
    private String map;
    
    private String language;
    
    private String distanceUnit;
    
    private String speedUnit;
    
    private double latitude;
    
    private double longitude;
    
    private int zoom;

    @Override
    public JsonObject toJson() {
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("id", id);
        json.add("email", email);
        json.add("password", password);
        json.add("readonly", readonly);
        json.add("admin", admin);
        json.add("map", map);
        json.add("language", language);
        json.add("distanceUnit", distanceUnit);
        json.add("speedUnit", speedUnit);
        json.add("latitude", latitude);
        json.add("longitude", longitude);
        json.add("zoom", zoom);
        return json.build();
    }

    @Override
    public void fromJson(JsonObject json) {
        id = json.getJsonNumber("id").longValue();
        email = json.getString("email");
        password = json.getString("password");
        readonly = json.getBoolean("readonly");
        admin = json.getBoolean("admin");
        map = json.getString("map");
        language = json.getString("language");
        distanceUnit = json.getString("distanceUnit");
        speedUnit = json.getString("speedUnit");
        latitude = json.getJsonNumber("latitude").doubleValue();
        longitude = json.getJsonNumber("longitude").doubleValue();
        zoom = json.getJsonNumber("zoom").intValue();
    }

}
