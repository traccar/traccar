/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.BaseDataHandler;
import org.traccar.Context;
import org.traccar.database.DataManager;
import org.traccar.model.Position;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@ChannelHandler.Sharable
public class DefaultDataHandler extends BaseDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataHandler.class);

    private final DataManager dataManager;

    private final JedisPool jedisPool;

    public DefaultDataHandler(DataManager dataManager) {
        this.dataManager = dataManager;
        this.jedisPool = new JedisPool("redis.pinme.io");
    }

    @Override
    protected Position handlePosition(Position position) {

        try (Jedis jedis = jedisPool.getResource()) {
            dataManager.addObject(position);
            LOGGER.warn("redis position id would be {}", jedis.incr("dbid"));
        } catch(com.mysql.cj.jdbc.exceptions.MysqlDataTruncation error) {
            LOGGER.warn("Failed to store position, deviceId: {}, {}", position.getDeviceId(), error.getMessage());
        } catch (Exception error) {
            LOGGER.error("Failed to store position", error);
            try {
                LOGGER.error(Context.getObjectMapper().writeValueAsString(position));
            } catch (JsonProcessingException e) {
                LOGGER.error("Error serializing", e);
            }
        }

        if (position.getAttributes().containsKey("source") && position.getAttributes().get("source").equals("import")) {
            LOGGER.warn("saved imported position {} for device {}", position.getId(), position.getDeviceId());
        }

        return position;
    }

}
