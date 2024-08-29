/*
 * Copyright 2017 - 2024 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.handler;

import jakarta.inject.Inject;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Attribute;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;

public class ComputedAttributesHandler extends BasePositionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputedAttributesHandler.class);

    private final CacheManager cacheManager;

    private final JexlEngine engine;

    private final JexlFeatures features;

    private final boolean includeDeviceAttributes;
    private final boolean includeLastAttributes;

    @Inject
    public ComputedAttributesHandler(Config config, CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        JexlSandbox sandbox = new JexlSandbox(false);
        sandbox.allow("com.safe.Functions");
        sandbox.allow(Math.class.getName());
        List.of(
            Double.class, Float.class, Integer.class, Long.class, Short.class,
            Character.class, Boolean.class, String.class, Byte.class, Date.class)
                .forEach((type) -> sandbox.allow(type.getName()));
        features = new JexlFeatures()
                .localVar(config.getBoolean(Keys.PROCESSING_COMPUTED_ATTRIBUTES_LOCAL_VARIABLES))
                .loops(config.getBoolean(Keys.PROCESSING_COMPUTED_ATTRIBUTES_LOOPS))
                .newInstance(config.getBoolean(Keys.PROCESSING_COMPUTED_ATTRIBUTES_NEW_INSTANCE_CREATION))
                .structuredLiteral(true);
        engine = new JexlBuilder()
                .strict(true)
                .namespaces(Collections.singletonMap("math", Math.class))
                .sandbox(sandbox)
                .create();
        includeDeviceAttributes = config.getBoolean(Keys.PROCESSING_COMPUTED_ATTRIBUTES_DEVICE_ATTRIBUTES);
        includeLastAttributes = config.getBoolean(Keys.PROCESSING_COMPUTED_ATTRIBUTES_LAST_ATTRIBUTES);
    }

    private MapContext prepareContext(Position position) {
        MapContext result = new MapContext();
        if (includeDeviceAttributes) {
            Device device = cacheManager.getObject(Device.class, position.getDeviceId());
            if (device != null) {
                for (String key : device.getAttributes().keySet()) {
                    result.set(key, device.getAttributes().get(key));
                }
            }
        }
        Position last = null;
        if (includeLastAttributes) {
            last = cacheManager.getPosition(position.getDeviceId());
        }
        Set<Method> methods = new HashSet<>(Arrays.asList(position.getClass().getMethods()));
        Arrays.asList(Object.class.getMethods()).forEach(methods::remove);
        for (Method method : methods) {
            if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
                String name = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);

                try {
                    if (!method.getReturnType().equals(Map.class)) {
                        result.set(name, method.invoke(position));
                        if (last != null) {
                            result.set(prefixAttribute("last", name), method.invoke(last));
                        }
                    } else {
                        for (Map.Entry<?, ?> entry : ((Map<?, ?>) method.invoke(position)).entrySet()) {
                            result.set((String) entry.getKey(), entry.getValue());
                        }
                        if (last != null) {
                            for (Map.Entry<?, ?> entry : ((Map<?, ?>) method.invoke(last)).entrySet()) {
                                result.set(prefixAttribute("last", (String) entry.getKey()), entry.getValue());
                            }
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException error) {
                    LOGGER.warn("Attribute reflection error", error);
                }
            }
        }
        return result;
    }

    private String prefixAttribute(String prefix, String key) {
        return prefix + Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }

    /**
     * @deprecated logic needs to be extracted to be used in API resource
     */
    @Deprecated
    public Object computeAttribute(Attribute attribute, Position position) throws JexlException {
        return engine
                .createScript(features, engine.createInfo(), attribute.getExpression())
                .execute(prepareContext(position));
    }

    @Override
    public void handlePosition(Position position, Callback callback) {
        var attributes = cacheManager.getDeviceObjects(position.getDeviceId(), Attribute.class).stream()
                .sorted(Comparator.comparing(Attribute::getPriority).reversed())
                .toList();
        for (Attribute attribute : attributes) {
            if (attribute.getAttribute() != null) {
                try {
                    Object result = computeAttribute(attribute, position);
                    if (result != null) {
                        switch (attribute.getAttribute()) {
                            case "valid" -> position.setValid((Boolean) result);
                            case "latitude" -> position.setLatitude(((Number) result).doubleValue());
                            case "longitude" -> position.setLongitude(((Number) result).doubleValue());
                            case "altitude" -> position.setAltitude(((Number) result).doubleValue());
                            case "speed" -> position.setSpeed(((Number) result).doubleValue());
                            case "course" -> position.setCourse(((Number) result).doubleValue());
                            case "address" -> position.setAddress((String) result);
                            case "accuracy" -> position.setAccuracy(((Number) result).doubleValue());
                            default -> {
                                switch (attribute.getType()) {
                                    case "number" -> {
                                        Number numberValue = (Number) result;
                                        position.getAttributes().put(attribute.getAttribute(), numberValue);
                                    }
                                    case "boolean" -> {
                                        Boolean booleanValue = (Boolean) result;
                                        position.getAttributes().put(attribute.getAttribute(), booleanValue);
                                    }
                                    default -> {
                                        position.getAttributes().put(attribute.getAttribute(), result.toString());
                                    }
                                }
                            }
                        }
                    } else {
                        position.getAttributes().remove(attribute.getAttribute());
                    }
                } catch (JexlException error) {
                    LOGGER.warn("Attribute computation error", error);
                } catch (ClassCastException error) {
                    LOGGER.warn("Attribute cast error", error);
                }
            }
        }
        callback.processed(false);
    }

}
