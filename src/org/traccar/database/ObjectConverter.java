/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.database;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.traccar.helper.Log;

public class ObjectConverter {
        
    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");

    public static JsonArray arrayToJson(Collection<? extends Convertable> collection) {
        
        JsonArrayBuilder array = Json.createArrayBuilder();
        
        for (Convertable object : collection) {
            array.add(object.toJson());
        }
        
        return array.build();
    }
    
    public static String getString(JsonObject json, String key) {
        if (json.containsKey(key)) {
            return json.getString(key);
        }
        return null;
    }
    
    public static void putString(JsonObjectBuilder json, String key, String value) {
        if (value != null) {
            json.add(key, value);
        }
    }
    
    public static long getLong(JsonObject json, String key) {
        if (json.containsKey(key)) {
            return json.getJsonNumber(key).longValue();
        }
        return 0;
    }
    
    public static void putLong(JsonObjectBuilder json, String key, long value) {
        json.add(key, value);
    }

    public static Date getDate(JsonObject json, String key) {
        if (json.containsKey(key)) {
            try {
                return dateFormat.parse(json.getString(key));
            } catch (ParseException error) {
                Log.warning(error);
            }
        }
        return null;
    }
    
    public static void putDate(JsonObjectBuilder json, String key, Date value) {
        if (value != null) {
            json.add(key, dateFormat.format(value));
        }
    }

}
