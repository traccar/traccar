/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Permission;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnusedReturnValue")
public final class QueryBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryBuilder.class);

    private final Config config;
    private final ObjectMapper objectMapper;

    private final Map<String, List<Integer>> indexMap = new HashMap<>();
    private Connection connection;
    private PreparedStatement statement;
    private final String query;
    private final boolean returnGeneratedKeys;

    private QueryBuilder(
            Config config, DataSource dataSource, ObjectMapper objectMapper,
            String query, boolean returnGeneratedKeys) throws SQLException {
        this.config = config;
        this.objectMapper = objectMapper;
        this.query = query;
        this.returnGeneratedKeys = returnGeneratedKeys;
        if (query != null) {
            connection = dataSource.getConnection();
            String parsedQuery = parse(query.trim(), indexMap);
            try {
                if (returnGeneratedKeys) {
                    statement = connection.prepareStatement(parsedQuery, Statement.RETURN_GENERATED_KEYS);
                } else {
                    statement = connection.prepareStatement(parsedQuery);
                }
            } catch (SQLException error) {
                connection.close();
                throw error;
            }
        }
    }

    private static String parse(String query, Map<String, List<Integer>> paramMap) {

        int length = query.length();
        StringBuilder parsedQuery = new StringBuilder(length);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int index = 1;

        for (int i = 0; i < length; i++) {

            char c = query.charAt(i);

            // String end
            if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false;
                }
            } else if (inDoubleQuote) {
                if (c == '"') {
                    inDoubleQuote = false;
                }
            } else {

                // String begin
                if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == ':' && i + 1 < length
                        && Character.isJavaIdentifierStart(query.charAt(i + 1))) {

                    // Identifier name
                    int j = i + 2;
                    while (j < length && Character.isJavaIdentifierPart(query.charAt(j))) {
                        j++;
                    }

                    String name = query.substring(i + 1, j);
                    c = '?';
                    i += name.length();
                    name = name.toLowerCase();

                    // Add to list
                    List<Integer> indexList = paramMap.computeIfAbsent(name, k -> new LinkedList<>());
                    indexList.add(index);

                    index++;
                }
            }

            parsedQuery.append(c);
        }

        return parsedQuery.toString();
    }

    public static QueryBuilder create(
            Config config, DataSource dataSource, ObjectMapper objectMapper, String query) throws SQLException {
        return new QueryBuilder(config, dataSource, objectMapper, query, false);
    }

    public static QueryBuilder create(
            Config config, DataSource dataSource, ObjectMapper objectMapper, String query,
            boolean returnGeneratedKeys) throws SQLException {
        return new QueryBuilder(config, dataSource, objectMapper, query, returnGeneratedKeys);
    }

    private List<Integer> indexes(String name) {
        name = name.toLowerCase();
        List<Integer> result = indexMap.get(name);
        if (result == null) {
            result = new LinkedList<>();
        }
        return result;
    }

    public QueryBuilder setBoolean(String name, boolean value) throws SQLException {
        for (int i : indexes(name)) {
            try {
                statement.setBoolean(i, value);
            } catch (SQLException error) {
                statement.close();
                connection.close();
                throw error;
            }
        }
        return this;
    }

    public QueryBuilder setInteger(String name, int value) throws SQLException {
        for (int i : indexes(name)) {
            try {
                statement.setInt(i, value);
            } catch (SQLException error) {
                statement.close();
                connection.close();
                throw error;
            }
        }
        return this;
    }

    public QueryBuilder setLong(String name, long value) throws SQLException {
        if (name.matches("^deviceId\\d+$")) {
            return setLong(name, value, true);
        }
        return setLong(name, value, false);
    }

    public QueryBuilder setLong(String name, long value, boolean nullIfZero) throws SQLException {
        for (int i : indexes(name)) {
            try {
                if (value == 0 && nullIfZero) {
                    statement.setNull(i, Types.INTEGER);
                } else {
                    statement.setLong(i, value);
                }
            } catch (SQLException error) {
                statement.close();
                connection.close();
                throw error;
            }
        }
        return this;
    }

    public QueryBuilder setDouble(String name, double value) throws SQLException {
        for (int i : indexes(name)) {
            try {
                statement.setDouble(i, value);
            } catch (SQLException error) {
                statement.close();
                connection.close();
                throw error;
            }
        }
        return this;
    }

    public QueryBuilder setString(String name, String value) throws SQLException {
        for (int i : indexes(name)) {
            try {
                if (value == null) {
                    statement.setNull(i, Types.VARCHAR);
                } else {
                    statement.setString(i, value);
                }
            } catch (SQLException error) {
                statement.close();
                connection.close();
                throw error;
            }
        }
        return this;
    }

    public QueryBuilder setDate(String name, Date value) throws SQLException {
        for (int i : indexes(name)) {
            try {
                if (value == null) {
                    statement.setNull(i, Types.TIMESTAMP);
                } else {
                    statement.setTimestamp(i, new Timestamp(value.getTime()));
                }
            } catch (SQLException error) {
                statement.close();
                connection.close();
                throw error;
            }
        }
        return this;
    }

    public QueryBuilder setBlob(String name, byte[] value) throws SQLException {
        for (int i : indexes(name)) {
            try {
                if (value == null) {
                    statement.setNull(i, Types.BLOB);
                } else {
                    statement.setBytes(i, value);
                }
            } catch (SQLException error) {
                statement.close();
                connection.close();
                throw error;
            }
        }
        return this;
    }

    public QueryBuilder setValue(String name, Object value) throws SQLException {
        if (value instanceof Boolean booleanValue) {
            setBoolean(name, booleanValue);
        } else if (value instanceof Integer integerValue) {
            setInteger(name, integerValue);
        } else if (value instanceof Long longValue) {
            setLong(name, longValue);
        } else if (value instanceof Double doubleValue) {
            setDouble(name, doubleValue);
        } else if (value instanceof String stringValue) {
            setString(name, stringValue);
        } else if (value instanceof Date dateValue) {
            setDate(name, dateValue);
        }
        return this;
    }

    public QueryBuilder setObject(Object object, List<String> columns) throws SQLException {

        try {
            for (String column : columns) {
                Method method = object.getClass().getMethod(
                        "get" + Character.toUpperCase(column.charAt(0)) + column.substring(1));
                if (method.getReturnType().equals(boolean.class)) {
                    setBoolean(column, (Boolean) method.invoke(object));
                } else if (method.getReturnType().equals(int.class)) {
                    setInteger(column, (Integer) method.invoke(object));
                } else if (method.getReturnType().equals(long.class)) {
                    setLong(column, (Long) method.invoke(object), column.endsWith("Id"));
                } else if (method.getReturnType().equals(double.class)) {
                    setDouble(column, (Double) method.invoke(object));
                } else if (method.getReturnType().equals(String.class)) {
                    setString(column, (String) method.invoke(object));
                } else if (method.getReturnType().equals(Date.class)) {
                    setDate(column, (Date) method.invoke(object));
                } else if (method.getReturnType().equals(byte[].class)) {
                    setBlob(column, (byte[]) method.invoke(object));
                } else {
                    setString(column, objectMapper.writeValueAsString(method.invoke(object)));
                }
            }
        } catch (ReflectiveOperationException | JsonProcessingException e) {
            LOGGER.warn("Set object error", e);
        }

        return this;
    }

    private interface ResultSetProcessor<T> {
        void process(T object, ResultSet resultSet) throws SQLException;
    }

    private <T> void addProcessors(
            List<ResultSetProcessor<T>> processors,
            final Class<?> parameterType, final Method method, final String name) {

        if (parameterType.equals(boolean.class)) {
            processors.add((object, resultSet) -> {
                try {
                    method.invoke(object, resultSet.getBoolean(name));
                } catch (IllegalAccessException | InvocationTargetException error) {
                    LOGGER.warn("Set property error", error);
                }
            });
        } else if (parameterType.equals(int.class)) {
            processors.add((object, resultSet) -> {
                try {
                    method.invoke(object, resultSet.getInt(name));
                } catch (IllegalAccessException | InvocationTargetException error) {
                    LOGGER.warn("Set property error", error);
                }
            });
        } else if (parameterType.equals(long.class)) {
            processors.add((object, resultSet) -> {
                try {
                    method.invoke(object, resultSet.getLong(name));
                } catch (IllegalAccessException | InvocationTargetException error) {
                    LOGGER.warn("Set property error", error);
                }
            });
        } else if (parameterType.equals(double.class)) {
            processors.add((object, resultSet) -> {
                try {
                    method.invoke(object, resultSet.getDouble(name));
                } catch (IllegalAccessException | InvocationTargetException error) {
                    LOGGER.warn("Set property error", error);
                }
            });
        } else if (parameterType.equals(String.class)) {
            processors.add((object, resultSet) -> {
                try {
                    method.invoke(object, resultSet.getString(name));
                } catch (IllegalAccessException | InvocationTargetException error) {
                    LOGGER.warn("Set property error", error);
                }
            });
        } else if (parameterType.equals(Date.class)) {
            processors.add((object, resultSet) -> {
                try {
                    Timestamp timestamp = resultSet.getTimestamp(name);
                    if (timestamp != null) {
                        method.invoke(object, new Date(timestamp.getTime()));
                    }
                } catch (IllegalAccessException | InvocationTargetException error) {
                    LOGGER.warn("Set property error", error);
                }
            });
        } else if (parameterType.equals(byte[].class)) {
            processors.add((object, resultSet) -> {
                try {
                    method.invoke(object, resultSet.getBytes(name));
                } catch (IllegalAccessException | InvocationTargetException error) {
                    LOGGER.warn("Set property error", error);
                }
            });
        } else {
            processors.add((object, resultSet) -> {
                String value = resultSet.getString(name);
                if (value != null && !value.isEmpty()) {
                    try {
                        method.invoke(object, objectMapper.readValue(value, parameterType));
                    } catch (InvocationTargetException | IllegalAccessException | IOException error) {
                        LOGGER.warn("Set property error", error);
                    }
                }
            });
        }
    }

    private void logQuery() {
        if (config.getBoolean(Keys.LOGGER_QUERIES)) {
            LOGGER.info(query);
        }
    }

    public <T> List<T> executeQuery(Class<T> clazz) throws SQLException {
        List<T> result = new LinkedList<>();

        if (query != null) {

            try {

                logQuery();

                try (ResultSet resultSet = statement.executeQuery()) {

                    ResultSetMetaData resultMetaData = resultSet.getMetaData();

                    List<ResultSetProcessor<T>> processors = new LinkedList<>();

                    Method[] methods = clazz.getMethods();

                    for (final Method method : methods) {
                        if (method.getName().startsWith("set") && method.getParameterTypes().length == 1) {

                            final String name = method.getName().substring(3);

                            // Check if column exists
                            boolean column = false;
                            for (int i = 1; i <= resultMetaData.getColumnCount(); i++) {
                                if (name.equalsIgnoreCase(resultMetaData.getColumnLabel(i))) {
                                    column = true;
                                    break;
                                }
                            }
                            if (!column) {
                                continue;
                            }

                            addProcessors(processors, method.getParameterTypes()[0], method, name);
                        }
                    }

                    while (resultSet.next()) {
                        try {
                            T object = clazz.getDeclaredConstructor().newInstance();
                            for (ResultSetProcessor<T> processor : processors) {
                                processor.process(object, resultSet);
                            }
                            result.add(object);
                        } catch (ReflectiveOperationException e) {
                            throw new IllegalArgumentException();
                        }
                    }
                }

            } finally {
                statement.close();
                connection.close();
            }
        }

        return result;
    }

    public long executeUpdate() throws SQLException {

        if (query != null) {
            try {
                logQuery();
                statement.execute();
                if (returnGeneratedKeys) {
                    ResultSet resultSet = statement.getGeneratedKeys();
                    if (resultSet.next()) {
                        return resultSet.getLong(1);
                    }
                }
            } finally {
                statement.close();
                connection.close();
            }
        }
        return 0;
    }

    public List<Permission> executePermissionsQuery() throws SQLException {
        List<Permission> result = new LinkedList<>();
        if (query != null) {
            try {
                logQuery();
                try (ResultSet resultSet = statement.executeQuery()) {
                    ResultSetMetaData resultMetaData = resultSet.getMetaData();
                    while (resultSet.next()) {
                        LinkedHashMap<String, Long> map = new LinkedHashMap<>();
                        for (int i = 1; i <= resultMetaData.getColumnCount(); i++) {
                            String label = resultMetaData.getColumnLabel(i);
                            map.put(label, resultSet.getLong(label));
                        }
                        result.add(new Permission(map));
                    }
                }
            } finally {
                statement.close();
                connection.close();
            }
        }

        return result;
    }

}
