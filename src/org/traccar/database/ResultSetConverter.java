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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

public class ResultSetConverter {
    
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");

    public static JsonArray convert(ResultSet rs) throws SQLException {

        JsonArrayBuilder json = Json.createArrayBuilder();
        ResultSetMetaData rsmd = rs.getMetaData();

        while (rs.next()) {

            int numColumns = rsmd.getColumnCount();
            JsonObjectBuilder obj = Json.createObjectBuilder();

            for (int i = 1; i <= numColumns; i++) {

                String columnName = rsmd.getColumnName(i).toLowerCase();

                switch (rsmd.getColumnType(i)) {
                    case java.sql.Types.BIGINT:
                        obj.add(columnName, rs.getInt(columnName));
                        break;
                    case java.sql.Types.BOOLEAN:
                        obj.add(columnName, rs.getBoolean(columnName));
                        break;
                    case java.sql.Types.DOUBLE:
                        obj.add(columnName, rs.getDouble(columnName));
                        break;
                    case java.sql.Types.FLOAT:
                        obj.add(columnName, rs.getFloat(columnName));
                        break;
                    case java.sql.Types.INTEGER:
                        obj.add(columnName, rs.getInt(columnName));
                        break;
                    case java.sql.Types.NVARCHAR:
                        obj.add(columnName, rs.getNString(columnName));
                        break;
                    case java.sql.Types.VARCHAR:
                        obj.add(columnName, rs.getString(columnName));
                        break;
                    case java.sql.Types.DATE:
                        obj.add(columnName, dateFormat.format(rs.getDate(columnName)));
                        break;
                    case java.sql.Types.TIMESTAMP:
                        obj.add(columnName, dateFormat.format(rs.getTimestamp(columnName)));
                        break;
                    default:
                        break;
                }
            }

            json.add(obj.build());
        }

        return json.build();
    }
}
