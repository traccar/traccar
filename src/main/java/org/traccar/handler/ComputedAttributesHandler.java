/*
 * Copyright 2017 - 2023 Anton Tananaev (anton@traccar.org)
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.netty.channel.ChannelHandler;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.MapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseDataHandler;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Attribute;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
public class ComputedAttributesHandler extends BaseDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputedAttributesHandler.class);

    private final CacheManager cacheManager;

    private final JexlEngine engine;

    private final JexlFeatures features;

    private final boolean includeDeviceAttributes;

    @Inject
    public ComputedAttributesHandler(Config config, CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        JexlSandbox sandbox = new JexlSandbox(false);
        sandbox.allow("com.safe.Functions");
        sandbox.allow(Math.class.getName());
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
        Set<Method> methods = new HashSet<>(Arrays.asList(position.getClass().getMethods()));
        Arrays.asList(Object.class.getMethods()).forEach(methods::remove);
        for (Method method : methods) {
            if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
                String name = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);

                try {
                    if (!method.getReturnType().equals(Map.class)) {
                        result.set(name, method.invoke(position));
                    } else {
                        for (Object key : ((Map<?, ?>) method.invoke(position)).keySet()) {
                            result.set((String) key, ((Map<?, ?>) method.invoke(position)).get(key));
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException error) {
                    LOGGER.warn("Attribute reflection error", error);
                }
            }
        }
        return result;
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
    protected Position handlePosition(Position position) {
        Collection<Attribute> attributes = cacheManager.getDeviceObjects(position.getDeviceId(), Attribute.class);
        for (Attribute attribute : attributes) {
            if (attribute.getAttribute() != null) {
                Object result = null;
                try {
                    result = computeAttribute(attribute, position);
                } catch (JexlException error) {
                    LOGGER.warn("Attribute computation error", error);
                }
                if (result != null) {
                    try {
                        switch (attribute.getAttribute()) {
                            case "valid":
                                position.setValid((Boolean) result);
                                break;
                            case "latitude":
                                position.setLatitude(((Number) result).doubleValue());
                                break;
                            case "longitude":
                                position.setLongitude(((Number) result).doubleValue());
                                break;
                            case "altitude":
                                position.setAltitude(((Number) result).doubleValue());
                                break;
                            case "speed":
                                position.setSpeed(((Number) result).doubleValue());
                                break;
                            case "course":
                                position.setCourse(((Number) result).doubleValue());
                                break;
                            case "address":
                                position.setAddress((String) result);
                                break;
                            case "accuracy":
                                position.setAccuracy(((Number) result).doubleValue());
                                break;
                            default:
                                switch (attribute.getType()) {
                                    case "number":
                                        Number numberValue = (Number) result;
                                        position.getAttributes().put(attribute.getAttribute(), numberValue);
                                        break;
                                    case "boolean":
                                        Boolean booleanValue = (Boolean) result;
                                        position.getAttributes().put(attribute.getAttribute(), booleanValue);
                                        break;
                                    default:
                                        position.getAttributes().put(attribute.getAttribute(), result.toString());
                                }
                                break;
                        }
                    } catch (ClassCastException error) {
                        LOGGER.warn("Attribute cast error", error);
                    }
                }
            }
        }
        return position;
    }

}
