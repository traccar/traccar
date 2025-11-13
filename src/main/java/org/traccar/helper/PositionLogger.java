/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.ChannelHandlerContext;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class PositionLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(PositionLogger.class);

    private final CacheManager cacheManager;
    private final Set<String> logAttributes = new LinkedHashSet<>();

    @Inject
    public PositionLogger(Config config, CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        logAttributes.addAll(Arrays.asList(config.getString(Keys.LOGGER_ATTRIBUTES).split("[, ]")));
    }

    public void log(ChannelHandlerContext context, Position position) {
        Device device = cacheManager.getObject(Device.class, position.getDeviceId());

        StringBuilder builder = new StringBuilder();
        builder.append("[").append(NetworkUtil.session(context.channel())).append("] ");
        builder.append("id: ").append(device.getUniqueId());
        for (String attribute : logAttributes) {
            switch (attribute) {
                case "time":
                    builder.append(", time: ").append(DateUtil.formatDate(position.getFixTime(), false));
                    break;
                case "position":
                    builder.append(", lat: ").append(String.format("%.5f", position.getLatitude()));
                    builder.append(", lon: ").append(String.format("%.5f", position.getLongitude()));
                    break;
                case "speed":
                    if (position.getSpeed() > 0) {
                        builder.append(", speed: ").append(String.format("%.1f", position.getSpeed()));
                    }
                    break;
                case "course":
                    builder.append(", course: ").append(String.format("%.1f", position.getCourse()));
                    break;
                case "accuracy":
                    if (position.getAccuracy() > 0) {
                        builder.append(", accuracy: ").append(String.format("%.1f", position.getAccuracy()));
                    }
                    break;
                case "outdated":
                    if (position.getOutdated()) {
                        builder.append(", outdated");
                    }
                    break;
                case "invalid":
                    if (!position.getValid()) {
                        builder.append(", invalid");
                    }
                    break;
                default:
                    Object value = position.getAttributes().get(attribute);
                    if (value != null) {
                        builder.append(", ").append(attribute).append(": ").append(value);
                    }
                    break;
            }
        }
        LOGGER.info(builder.toString());
    }

}
