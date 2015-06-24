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
package org.traccar.http;

import java.beans.Introspector;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import javax.json.JsonValue;

import org.traccar.helper.IgnoreOnSerialization;
import org.traccar.model.Factory;

public class JsonConverter {

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public static Date parseDate(String value) throws ParseException {
        return dateFormat.parse(value);
    }

    public static <T extends Factory> T objectFromJson(Reader reader, T prototype) throws ParseException {
        return objectFromJson(Json.createReader(reader).readObject(), prototype);
    }

    public static <T> T enumObjectFromJson(Reader reader, EnumFactory<? extends Enum<?>> factory) throws ParseException {
        JsonObject json = Json.createReader(reader).readObject();
        T object = factory.<T>create(json);
        populateObject(json, object);
        return object;
    }

    public static <T extends Factory> T objectFromJson(JsonObject json, T prototype) throws ParseException {
        T object = (T) prototype.create();
        populateObject(json, object);
        return object;
    }

    private static void populateObject(JsonObject json, Object object) throws ParseException {
        Method[] methods = object.getClass().getMethods();

        for (final Method method : methods) {
            if (method.getName().startsWith("set") && method.getParameterTypes().length == 1) {

                final String name = Introspector.decapitalize(method.getName().substring(3));
                Class<?> parameterType = method.getParameterTypes()[0];

                if (json.containsKey(name)) try {
                    if (parameterType.equals(boolean.class)) {
                        method.invoke(object, json.getBoolean(name));
                    } else if (parameterType.equals(int.class)) {
                        method.invoke(object, json.getJsonNumber(name).intValue());
                    } else if (parameterType.equals(long.class)) {
                        if (json.get(name).getValueType() == JsonValue.ValueType.NUMBER) {
                            method.invoke(object, json.getJsonNumber(name).longValue());
                        }
                    } else if (parameterType.equals(double.class)) {
                        method.invoke(object, json.getJsonNumber(name).doubleValue());
                    } else if (parameterType.equals(String.class)) {
                        method.invoke(object, json.getString(name));
                    } else if (parameterType.equals(Date.class)) {
                        method.invoke(object, dateFormat.parse(json.getString(name)));
                    } else if (parameterType.isEnum()) {
                        method.invoke(object, Enum.valueOf((Class<? extends Enum>) parameterType, json.getString(name)));
                    } else {
                        Object nestedObject = parameterType.newInstance();
                        populateObject(json.getJsonObject(name), nestedObject);
                        method.invoke(object, nestedObject);
                    }
                } catch (IllegalAccessException error) {
                } catch (InvocationTargetException error) {
                } catch (InstantiationException e) {
                }
            }
        }

    }

    public static <T> JsonObject objectToJson(T object) {

        JsonObjectBuilder json = Json.createObjectBuilder();

        Method[] methods = object.getClass().getMethods();

        for (Method method : methods) {
            if(method.isAnnotationPresent(IgnoreOnSerialization.class)) {
                continue;
            }
            if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
                String name = Introspector.decapitalize(method.getName().substring(3));
                try {
                    if (method.getReturnType().equals(boolean.class)) {
                        json.add(name, (Boolean) method.invoke(object));
                    } else if (method.getReturnType().equals(int.class)) {
                        json.add(name, (Integer) method.invoke(object));
                    } else if (method.getReturnType().equals(long.class)) {
                        json.add(name, (Long) method.invoke(object));
                    } else if (method.getReturnType().equals(double.class)) {
                        json.add(name, (Double) method.invoke(object));
                    } else if (method.getReturnType().equals(String.class)) {
                        String value = (String) method.invoke(object);
                        if (value != null) {
                            json.add(name, value);
                        }
                    } else if (method.getReturnType().equals(Date.class)) {
                        Date value = (Date) method.invoke(object);
                        if (value != null) {
                            json.add(name, dateFormat.format(value));
                        }
                    }
                } catch (IllegalAccessException error) {
                } catch (InvocationTargetException error) {
                }
            }
        }

        return json.build();
    }

    public static JsonArray arrayToJson(Collection<?> array) {

        JsonArrayBuilder json = Json.createArrayBuilder();

        for (Object object : array) {
            json.add(objectToJson(object));
        }

        return json.build();
    }

}
