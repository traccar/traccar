/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
import jakarta.inject.Singleton;
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
import org.traccar.helper.ReflectionCache;
import org.traccar.model.Attribute;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.lang.invoke.MethodHandle;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ComputedAttributesProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputedAttributesProvider.class);

    private final CacheManager cacheManager;

    private final JexlEngine engine;
    private final JexlFeatures features;

    private final boolean includeDeviceAttributes;
    private final boolean includeLastAttributes;

    @Inject
    public ComputedAttributesProvider(Config config, CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        JexlSandbox sandbox = new JexlSandbox(false);
        sandbox.allow("com.safe.Functions");
        sandbox.allow(Math.class.getName());
        List.of(
            Double.class, Float.class, Integer.class, Long.class, Short.class,
            Character.class, Boolean.class, String.class, Byte.class, Date.class,
            HashMap.class, LinkedHashMap.class, double[].class, int[].class, boolean[].class, String[].class)
                .forEach((type) -> sandbox.allow(type.getName()));
        features = new JexlFeatures()
                .localVar(config.getBoolean(Keys.PROCESSING_COMPUTED_ATTRIBUTES_LOCAL_VARIABLES))
                .loops(config.getBoolean(Keys.PROCESSING_COMPUTED_ATTRIBUTES_LOOPS))
                .newInstance(config.getBoolean(Keys.PROCESSING_COMPUTED_ATTRIBUTES_NEW_INSTANCE_CREATION))
                .structuredLiteral(true);
        engine = new JexlBuilder()
                .strict(true)
                .namespaces(Map.of("math", Math.class))
                .sandbox(sandbox)
                .create();
        includeDeviceAttributes = config.getBoolean(Keys.PROCESSING_COMPUTED_ATTRIBUTES_DEVICE_ATTRIBUTES);
        includeLastAttributes = config.getBoolean(Keys.PROCESSING_COMPUTED_ATTRIBUTES_LAST_ATTRIBUTES);
    }

    public Object compute(Attribute attribute, Position position) throws JexlException {
        return engine
                .createScript(features, engine.createInfo(), attribute.getExpression())
                .execute(prepareContext(position));
    }

    private MapContext prepareContext(Position position) {
        MapContext result = new MapContext();
        if (includeDeviceAttributes) {
            Device device = cacheManager.getObject(Device.class, position.getDeviceId());
            if (device != null) {
                for (var entry : device.getAttributes().entrySet()) {
                    result.set(entry.getKey(), entry.getValue());
                }
            }
        }
        Position last = includeLastAttributes ? cacheManager.getPosition(position.getDeviceId()) : null;
        ReflectionCache.getProperties(Position.class, "get").forEach((name, property) -> {
            MethodHandle handle = property.handle();
            try {
                Object positionValue = handle.invokeExact((Object) position);
                Object lastValue = last != null ? handle.invokeExact((Object) last) : null;
                if (!property.type().equals(Map.class)) {
                    result.set(name, positionValue);
                    if (last != null) {
                        result.set(prefixAttribute("last", name), lastValue);
                    }
                } else {
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) positionValue).entrySet()) {
                        result.set((String) entry.getKey(), entry.getValue());
                    }
                    if (last != null) {
                        for (Map.Entry<?, ?> entry : ((Map<?, ?>) lastValue).entrySet()) {
                            result.set(prefixAttribute("last", (String) entry.getKey()), entry.getValue());
                        }
                    }
                }
            } catch (Throwable error) {
                LOGGER.warn("Attribute reflection error", error);
            }
        });
        return result;
    }

    private String prefixAttribute(String prefix, String key) {
        return prefix + Character.toUpperCase(key.charAt(0)) + key.substring(1);
    }

}
