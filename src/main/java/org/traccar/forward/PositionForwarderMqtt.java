/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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

import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.MqttUtil;
import org.traccar.model.Position;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.nimbusds.oauth2.sdk.util.StringUtils;

public class PositionForwarderMqtt implements PositionForwarder {

    private final Mqtt3AsyncClient client;
    private final ObjectMapper objectMapper;
    private final String topic;

    /*
     * Device alias to be used when publishing data
     */
    private static final String MQTT_ALIAS = "mqtt.alias";

    public PositionForwarderMqtt(final Config config, final ObjectMapper objectMapper) {
        this.topic = config.getString(Keys.FORWARD_TOPIC);
        this.client = MqttUtil.createClient(config.getString(Keys.FORWARD_URL), topic + "/state");
        this.objectMapper = objectMapper;
    }

    @Override
    public void forward(final PositionData positionData, final ResultHandler resultHandler) {

        // If have an alias defined for the device publish only position information for
        // the device
        // otherwise publish full positionData which have position and device attributes
        final String alias = positionData.getDevice().getString(MQTT_ALIAS, "");
        if (StringUtils.isNotBlank(alias)) {
            publishPosition(topic + "/" + alias, positionData.getPosition(), resultHandler);
        }

        if (StringUtils.isBlank(alias)) {
            publish(topic, positionData, resultHandler);
        }

    }

    private void publishPosition(final String topic, final Position position, final ResultHandler resultHandler) {
            publish(topic, position, resultHandler);
    }

    private void publish(final String pubTopic, final Object object, final ResultHandler resultHandler) {
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            resultHandler.onResult(false, e);
            return;
        }
        MqttUtil.publish(client, pubTopic, payload, (message, e) -> resultHandler.onResult(e == null, e));
    }

}
