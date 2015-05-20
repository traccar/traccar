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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.traccar.model.Factory;

public class QueryBuilder {
    
    private final Map<String, List<Integer>> indexMap;
    private final PreparedStatement statement;
    
    private QueryBuilder(DataSource dataSource, String query) throws SQLException {
        indexMap = new HashMap<String, List<Integer>>();
        statement = dataSource.getConnection().prepareStatement(
                parse(query, indexMap), Statement.RETURN_GENERATED_KEYS);
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
                    List<Integer> indexList = paramMap.get(name);
                    if (indexList == null) {
                        indexList = new LinkedList<Integer>();
                        paramMap.put(name, indexList);
                    }
                    indexList.add(index);

                    index++;
                }
            }

            parsedQuery.append(c);
        }

        return parsedQuery.toString();
    }
    
    public static QueryBuilder create(DataSource dataSource, String query) throws SQLException {
        return new QueryBuilder(dataSource, query);
    }
    
    private List<Integer> indexes(String name) {
        name = name.toLowerCase();
        List<Integer> result = indexMap.get(name);
        if (result == null) {
            result = new LinkedList<Integer>();
        }
        return result;
    }
    
    public QueryBuilder setBoolean(String name, boolean value) throws SQLException {
        for (int i : indexes(name)) {
            statement.setBoolean(i, value);
        }
        return this;
    }
    
    public QueryBuilder setInteger(String name, int value) throws SQLException {
        for (int i : indexes(name)) {
            statement.setInt(i, value);
        }
        return this;
    }
    
    public QueryBuilder setLong(String name, long value) throws SQLException {
        for (int i : indexes(name)) {
            statement.setLong(i, value);
        }
        return this;
    }
    
    public QueryBuilder setDouble(String name, double value) throws SQLException {
        for (int i : indexes(name)) {
            statement.setDouble(i, value);
        }
        return this;
    }
    
    public QueryBuilder setString(String name, String value) throws SQLException {
        for (int i : indexes(name)) {
            if (value == null) {
                statement.setNull(i, Types.VARCHAR);
            } else {
                statement.setString(i, value);
            }
        }
        return this;
    }
    
    public QueryBuilder setDate(String name, Date value) throws SQLException {
        for (int i : indexes(name)) {
            if (value == null) {
                statement.setNull(i, Types.TIMESTAMP);
            } else {
                statement.setTimestamp(i, new Timestamp(value.getTime()));
            }
        }
        return this;
    }
    
    public QueryBuilder setObject(Object object) throws SQLException {
        
        Method[] methods = object.getClass().getMethods();
        
        for (Method method : methods) {
            if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
                String name = method.getName().substring(3);
                try {
                    if (method.getReturnType().equals(boolean.class)) {
                        setBoolean(name, (Boolean) method.invoke(object));
                    } else if (method.getReturnType().equals(int.class)) {
                        setInteger(name, (Integer) method.invoke(object));
                    } else if (method.getReturnType().equals(long.class)) {
                        setLong(name, (Long) method.invoke(object));
                    } else if (method.getReturnType().equals(double.class)) {
                        setDouble(name, (Double) method.invoke(object));
                    } else if (method.getReturnType().equals(String.class)) {
                        setString(name, (String) method.invoke(object));
                    } else if (method.getReturnType().equals(Date.class)) {
                        setDate(name, (Date) method.invoke(object));
                    }
                } catch (IllegalAccessException error) {
                } catch (InvocationTargetException error) {
                }
            }
        }
        
        return this;
    }
    
    private interface ResultSetProcessor<T> {
        public void process(T object, ResultSet resultSet) throws SQLException;
    }
    
    public <T extends Factory> Collection<T> executeQuery(T prototype) throws SQLException {
        List<T> result = new LinkedList<T>();
        
        ResultSet resultSet = statement.executeQuery();
        ResultSetMetaData resultMetaData = resultSet.getMetaData();
        
        List<ResultSetProcessor<T>> processors = new LinkedList<ResultSetProcessor<T>>();
        
        Method[] methods = prototype.getClass().getMethods();
        
        for (final Method method : methods) {
            if (method.getName().startsWith("set") && method.getParameterTypes().length == 1) {

                final String name = method.getName().substring(3);
                
                // Check if column exists
                boolean column = false;
                for (int i = 1; i <= resultMetaData.getColumnCount(); i++) {
                    if (name.equalsIgnoreCase(resultMetaData.getColumnName(i))) {
                        column = true;
                        break;
                    }
                }
                if (!column) {
                    continue;
                }
                
                Class<?> parameterType = method.getParameterTypes()[0];

                if (parameterType.equals(boolean.class)) {
                    processors.add(new ResultSetProcessor<T>() {
                        @Override
                        public void process(T object, ResultSet resultSet) throws SQLException {
                            try {
                                method.invoke(object, resultSet.getBoolean(name));
                            } catch (IllegalAccessException error) {
                            } catch (InvocationTargetException error) {
                            }
                        }
                    });
                } else if (parameterType.equals(int.class)) {
                    processors.add(new ResultSetProcessor<T>() {
                        @Override
                        public void process(T object, ResultSet resultSet) throws SQLException {
                            try {
                                method.invoke(object, resultSet.getInt(name));
                            } catch (IllegalAccessException error) {
                            } catch (InvocationTargetException error) {
                            }
                        }
                    });
                } else if (parameterType.equals(long.class)) {
                    processors.add(new ResultSetProcessor<T>() {
                        @Override
                        public void process(T object, ResultSet resultSet) throws SQLException {
                            try {
                                method.invoke(object, resultSet.getLong(name));
                            } catch (IllegalAccessException error) {
                            } catch (InvocationTargetException error) {
                            }
                        }
                    });
                } else if (parameterType.equals(double.class)) {
                    processors.add(new ResultSetProcessor<T>() {
                        @Override
                        public void process(T object, ResultSet resultSet) throws SQLException {
                            try {
                                method.invoke(object, resultSet.getDouble(name));
                            } catch (IllegalAccessException error) {
                            } catch (InvocationTargetException error) {
                            }
                        }
                    });
                } else if (parameterType.equals(String.class)) {
                    processors.add(new ResultSetProcessor<T>() {
                        @Override
                        public void process(T object, ResultSet resultSet) throws SQLException {
                            try {
                                method.invoke(object, resultSet.getString(name));
                            } catch (IllegalAccessException error) {
                            } catch (InvocationTargetException error) {
                            }
                        }
                    });
                } else if (parameterType.equals(Date.class)) {
                    processors.add(new ResultSetProcessor<T>() {
                        @Override
                        public void process(T object, ResultSet resultSet) throws SQLException {
                            try {
                                method.invoke(object, new Date(resultSet.getTimestamp(name).getTime()));
                            } catch (IllegalAccessException error) {
                            } catch (InvocationTargetException error) {
                            }
                        }
                    });
                }
            }
        }

        while (resultSet.next()) {
            T object = (T) prototype.create();
            for (ResultSetProcessor<T> processor : processors) {
                processor.process(object, resultSet);
            }
            result.add(object);
        }

        return result;
    }

    public long executeUpdate() throws SQLException {
        
        statement.executeUpdate();
        ResultSet resultSet = statement.getGeneratedKeys();
        if (resultSet.next()) {
            return resultSet.getLong(1);
        }
        return 0;
    }
    
}
