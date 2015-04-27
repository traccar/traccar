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
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.traccar.database.JsonConvertable;

public class Device implements JsonConvertable {

    private long id;
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    private String name;
    public String getName() { return uniqueId; }
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
        json.add("id", id);
        json.add("name", name);
        json.add("uniqueId", uniqueId);
        json.add("status", status);
        json.add("lastUpdate", dateFormat.format(lastUpdate));
        json.add("positionId", positionId);
        json.add("dataId", dataId);
        return json.build();
    }

    @Override
    public void fromJson(JsonObject json) throws ParseException {
        id = json.getJsonNumber("id").longValue();
        name = json.getString("name");
        uniqueId = json.getString("uniqueId");
        status = json.getString("status");
        lastUpdate = dateFormat.parse(json.getString("lastUpdate"));
        positionId = json.getJsonNumber("positionId").longValue();
        dataId = json.getJsonNumber("dataId").longValue();
    }

}
