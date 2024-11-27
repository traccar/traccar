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
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
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
    private final String jedisHost;

    public DefaultDataHandler(DataManager dataManager) {
        this.dataManager = dataManager;
        this.jedisHost = Context.getConfig().getString("public.redis.host", "inoredis.pinme.io");
        String redisPass = Context.getConfig().getString("public.redis.password");
        this.jedisPool = new JedisPool(new GenericObjectPoolConfig(), this.jedisHost, 6379, 2000, redisPass);
    }

    @Override
    protected Position handlePosition(Position position) {

        if(position.getId() > 0){
            return position;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            // dataManager.addObject(position);
            position.setId(jedis.incr("dbid"));
        } catch (Exception error) {
            LOGGER.error(String.format("%s trying to store position using %s", error.getMessage(), this.jedisHost));
            try {
                LOGGER.error(Context.getObjectMapper().writeValueAsString(position));
            } catch (JsonProcessingException e) {
                LOGGER.error("Error serializing", e);
            }
        }

        return position;
    }

}
