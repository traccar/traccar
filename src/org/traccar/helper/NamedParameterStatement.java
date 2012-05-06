/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.helper;

import java.sql.*;
import java.util.Date;
import java.util.*;

public class NamedParameterStatement {

    /**
     * Native statement
     */
    private PreparedStatement statement;

    /**
     * Index mapping
     */
    private final Map indexMap;

    /**
     * Query string
     */
    private final String parsedQuery;

    /**
     * Database connection
     */
    private AdvancedConnection connection;

    /**
     * Initialize statement
     */
    public NamedParameterStatement(AdvancedConnection connection, String query) {

        indexMap = new HashMap();
        parsedQuery = parse(query, indexMap);
        this.connection = connection;
    }

    /**
     * Parse query
     */
    static String parse(String query, Map paramMap) {

        int length = query.length();
        StringBuilder parsedQuery = new StringBuilder(length);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int index = 1;

        for(int i = 0; i < length; i++) {

            char c = query.charAt(i);

            // String end
            if (inSingleQuote) {
                if (c == '\'') inSingleQuote = false;
            } else if (inDoubleQuote) {
                if (c == '"') inDoubleQuote = false;
            } else {

                // String begin
                if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == ':' && i + 1 < length &&
                        Character.isJavaIdentifierStart(query.charAt(i + 1))) {

                    // Identifier name
                    int j = i + 2;
                    while (j < length && Character.isJavaIdentifierPart(query.charAt(j))) j++;

                    String name = query.substring(i + 1, j);
                    c = '?';
                    i += name.length();

                    // Add to list
                    List indexList = (List) paramMap.get(name);
                    if (indexList == null) {
                        indexList = new LinkedList();
                        paramMap.put(name, indexList);
                    }
                    indexList.add(new Integer(index));

                    index++;
                }
            }

            parsedQuery.append(c);
        }

        return parsedQuery.toString();
    }

    public void prepare() throws SQLException {
        try {
            if (statement == null) {
                statement = connection.getInstance().prepareStatement(parsedQuery);
            }
        } catch (SQLException error) {
            connection.reset();
            statement = connection.getInstance().prepareStatement(parsedQuery);
        }
    }

    /**
     * Execute query with result
     */
    public ResultSet executeQuery() throws SQLException {
        return statement.executeQuery();
    }


    /**
     * Executes query without result
     */
    public int executeUpdate() throws SQLException {
        return statement.executeUpdate();
    }

    /**
     * Return generated keys
     */
    public ResultSet getGeneratedKeys() throws SQLException {
        return statement.getGeneratedKeys();
    }

    /**
     * Immediately closes the statement
     */
    public void close() throws SQLException {
        statement.close();
    }

    public void setInt(String name, Integer value) throws SQLException {

        List indexList = (List) indexMap.get(name);
        if (indexList != null) for (Object index: indexList) {
            if (value != null) {
                statement.setInt((Integer) index, value);
            } else {
                statement.setNull((Integer) index, Types.INTEGER);
            }
        }
    }

    public void setLong(String name, Long value) throws SQLException {

        List indexList = (List) indexMap.get(name);
        if (indexList != null) for (Object index: indexList) {
            if (value != null) {
                statement.setLong((Integer) index, value);
            } else {
                statement.setNull((Integer) index, Types.INTEGER);
            }
        }
    }

    public void setBoolean(String name, Boolean value) throws SQLException {

        List indexList = (List) indexMap.get(name);
        if (indexList != null) for (Object index: indexList) {
            if (value != null) {
                statement.setBoolean((Integer) index, value);
            } else {
                statement.setNull((Integer) index, Types.BOOLEAN);
            }
        }
    }

    public void setDouble(String name, Double value) throws SQLException {

        List indexList = (List) indexMap.get(name);
        if (indexList != null) for (Object index: indexList) {
            if (value != null) {
                statement.setDouble((Integer) index, value);
            } else {
                statement.setNull((Integer) index, Types.DOUBLE);
            }
        }
    }

    public void setTimestamp(String name, Date value) throws SQLException {

        List indexList = (List) indexMap.get(name);
        if (indexList != null) for (Object index: indexList) {
            if (value != null) {
                statement.setTimestamp(
                        (Integer) index, new Timestamp(value.getTime()));
            } else {
                statement.setNull((Integer) index, Types.TIMESTAMP);
            }
        }
    }

    public void setString(String name, String value) throws SQLException {

        List indexList = (List) indexMap.get(name);
        if (indexList != null) for (Object index: indexList) {
            if (value != null) {
                statement.setString((Integer) index, value);
            } else {
                statement.setNull((Integer) index, Types.VARCHAR);
            }
        }
    }

}
