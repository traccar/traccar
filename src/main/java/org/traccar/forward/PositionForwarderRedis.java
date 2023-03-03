/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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
package org.traccar.forward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import redis.clients.jedis.Jedis;

public class PositionForwarderRedis implements PositionForwarder {

    private final String url;

    private final ObjectMapper objectMapper;

    public PositionForwarderRedis(Config config, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.url = config.getString(Keys.FORWARD_URL);
    }

    @Override
    public void forward(PositionData positionData, ResultHandler resultHandler) {

        try {
            String key = "positions." + positionData.getDevice().getUniqueId();
            String value = objectMapper.writeValueAsString(positionData.getPosition());
            try (Jedis jedis = new Jedis(url)) {
                jedis.lpush(key, value);
            }
            resultHandler.onResult(true, null);
        } catch (JsonProcessingException e) {
            resultHandler.onResult(false, e);
        }
    }

}
