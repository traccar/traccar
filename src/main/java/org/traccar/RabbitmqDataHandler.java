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
package org.traccar;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandler;
import org.traccar.database.IdentityManager;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.rabbitmq.RabbitmqManager;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ChannelHandler.Sharable
public class RabbitmqDataHandler extends BaseDataHandler {

    private static final String KEY_POSITION = "position";
    private static final String KEY_DEVICE = "device";

    private final IdentityManager identityManager;
    private final RabbitmqManager rabbitmqManager;

    @Inject
    public RabbitmqDataHandler(IdentityManager identityManager, RabbitmqManager rabbitmqManager) {
        this.identityManager = identityManager;
        this.rabbitmqManager = rabbitmqManager;
    }

    @Override
    protected Position handlePosition(Position position) {
        try {
            rabbitmqManager.sendToPositionsQueue(new ObjectMapper().writeValueAsString(prepareJsonPayload(position)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return position;
    }

    private Map<String, Object> prepareJsonPayload(Position position) {
        Map<String, Object> data = new HashMap<>();
        Device device = identityManager.getById(position.getDeviceId());
        data.put(KEY_POSITION, position);
        if (device != null) {
            data.put(KEY_DEVICE, device);
        }
        return data;
    }

}
