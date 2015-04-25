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
import java.text.SimpleDateFormat;
import java.util.Collection;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import org.traccar.model.Position;

public class ObjectConverter {
    
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
    
    public static JsonArray convert(Collection<Position> positions) {
        
        JsonArrayBuilder array = Json.createArrayBuilder();
        
        for (Position position : positions) {
            JsonObjectBuilder object = Json.createObjectBuilder();
            
            //object.add("id", position.getId());
            object.add("time", dateFormat.format(position.getTime()));
            object.add("valid", position.getValid());
            object.add("latitude", position.getLatitude());
            object.add("longitude", position.getLongitude());
            object.add("altitude", position.getAltitude());
            object.add("speed", position.getSpeed());
            object.add("course", position.getCourse());
            //object.add("other", position.getExtendedInfo());
            
            array.add(object.build());
        }
        
        return array.build();
    }
    
}
