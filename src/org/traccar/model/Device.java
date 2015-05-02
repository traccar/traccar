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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.traccar.database.Convertable;
import org.traccar.database.ObjectConverter;
import org.traccar.helper.Log;

public class Device implements Convertable, Factory {

    @Override
    public Device create() {
        return new Device();
    }

    private long id;
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String uniqueId;
    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }
    
    private String status;
    
    private Date lastUpdate;
    
    private long positionId;
    
    private long dataId;

    @Override
    public JsonObject toJson() {
        JsonObjectBuilder json = Json.createObjectBuilder();
        ObjectConverter.putLong(json, "id", id);
        ObjectConverter.putString(json, "name", name);
        ObjectConverter.putString(json, "uniqueId", uniqueId);
        ObjectConverter.putString(json, "status", status);
        ObjectConverter.putDate(json, "lastUpdate", lastUpdate);
        ObjectConverter.putLong(json, "positionId", positionId);
        ObjectConverter.putLong(json, "dataId", dataId);
        return json.build();
    }

    @Override
    public void fromJson(JsonObject json) throws ParseException {
        id = ObjectConverter.getLong(json, "id");
        name = ObjectConverter.getString(json, "name");
        uniqueId = ObjectConverter.getString(json, "uniqueId");
        status = ObjectConverter.getString(json, "status");
        lastUpdate = ObjectConverter.getDate(json, "lastUpdate");
        positionId = ObjectConverter.getLong(json, "positionId");
        dataId = ObjectConverter.getLong(json, "dataId");
    }

}
