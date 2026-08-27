/*
 * Copyright 2017 - 2026 Anton Tananaev (anton@traccar.org)
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
import org.apache.commons.jexl3.JexlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.Attribute;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.util.Comparator;

public class ComputedAttributesHandler extends BasePositionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputedAttributesHandler.class);

    private final CacheManager cacheManager;
    private final ComputedAttributesProvider provider;
    private final boolean early;

    public static class Early extends ComputedAttributesHandler {
        @Inject
        public Early(CacheManager cacheManager, ComputedAttributesProvider provider) {
            super(cacheManager, provider, true);
        }
    }

    public static class Late extends ComputedAttributesHandler {
        @Inject
        public Late(CacheManager cacheManager, ComputedAttributesProvider provider) {
            super(cacheManager, provider, false);
        }
    }

    public ComputedAttributesHandler(
            CacheManager cacheManager, ComputedAttributesProvider provider, boolean early) {
        this.cacheManager = cacheManager;
        this.provider = provider;
        this.early = early;
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        var attributes = cacheManager.getDeviceObjects(position.getDeviceId(), Attribute.class).stream()
                .filter(attribute -> attribute.getPriority() < 0 == early)
                .sorted(Comparator.comparing(Attribute::getPriority).reversed())
                .toList();
        for (Attribute attribute : attributes) {
            if (attribute.getAttribute() != null) {
                try {
                    Object result = provider.compute(attribute, position);
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
                        position.removeAttribute(attribute.getAttribute());
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
