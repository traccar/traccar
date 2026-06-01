/*
 * Copyright 2025 - 2026 Anton Tananaev (anton@traccar.org)
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

import org.traccar.storage.QueryIgnore;

import java.beans.Introspector;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflectionCache {

    private ReflectionCache() {}

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();
    private static final MethodType SETTER_TYPE = MethodType.methodType(void.class, Object.class, Object.class);
    private static final MethodType GETTER_TYPE = MethodType.methodType(Object.class, Object.class);

    private record Key(Class<?> clazz, String type) {}

    public record PropertyMethod(Class<?> type, boolean queryIgnore, String lowerCaseName, MethodHandle handle) {}

    private static final Map<Key, Map<String, PropertyMethod>> CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Constructor<?>> CONSTRUCTORS = new ConcurrentHashMap<>();

    public static Map<String, PropertyMethod> getProperties(Class<?> clazz, String type) {
        return CACHE.computeIfAbsent(new Key(clazz, type), key -> {
            Map<String, PropertyMethod> properties = new HashMap<>();
            for (Method method : key.clazz().getMethods()) {
                int parameterCount = key.type().equals("set") ? 1 : 0;
                var parameters = method.getParameterTypes();
                if (method.getName().startsWith(key.type()) && parameters.length == parameterCount
                        && !method.getName().equals("getClass")) {
                    String name = Introspector.decapitalize(method.getName().substring(3));
                    MethodHandle handle;
                    try {
                        handle = LOOKUP.unreflect(method)
                                .asType(parameterCount == 1 ? SETTER_TYPE : GETTER_TYPE);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                    Class<?> propertyType = parameterCount == 1 ? parameters[0] : method.getReturnType();
                    properties.put(name, new PropertyMethod(
                            propertyType, method.isAnnotationPresent(QueryIgnore.class),
                            name.toLowerCase(Locale.ROOT), handle));
                }
            }
            return properties;
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> getConstructor(Class<T> clazz) {
        return (Constructor<T>) CONSTRUCTORS.computeIfAbsent(clazz, key -> {
            try {
                return key.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        });
    }

}
