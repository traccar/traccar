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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
     * Device alias to be used when publising data
     */
    private static final String MQTT_ALIAS = "mqtt.alias";
    /**
     * Options for publishing
     * empty/not defined - combined data, and if alias defined position information
     * combined - combined data
     * alias - device position
     * deviceId - device position 
     * noalias - dont publish with alias even when alias defined
     */
	private static final String MQTT_FORWARD_OPTIONS = "mqtt.forward.options";

	/**
	 * Dont publish device information by alias
	 */
	private static final String OPTION_NOALIAS = "no_alias";
	/**
	 * publish all combined position data
	 */
	private static final String OPTION_COMBINED = "combined";
	/**
	 * publish by deviceId
	 */
	private static final String OPTION_DEVICE_ID = "deviceId";
	/**
	 * publish by alias (if {@link}MQTT_ALIAS is defined) 
	 */
	private static final String OPTION_ALIAS = "alias";
	/**
	 * publish device json data
	 */
	private static final String OPTION_DEVICE_JSON = "device_json";
    
    
    public PositionForwarderMqtt(Config config, ObjectMapper objectMapper) {
        this.topic = config.getString(Keys.FORWARD_TOPIC);
        this.client = MqttUtil.createClient(config.getString(Keys.FORWARD_URL), topic + "/state");
        this.objectMapper = objectMapper;
    }

    @Override
    public void forward(PositionData positionData, ResultHandler resultHandler) {

    	// get options and remove spaces
    	final String optionsSt = positionData.getDevice().getString(MQTT_FORWARD_OPTIONS, "").replaceAll(" ", "");
    	final Set<String> options = StringUtils.isBlank(optionsSt) ? Collections.emptySet() : new HashSet<>(Arrays.asList(optionsSt.split(",")));
    	
    	/**
         * If mqtt.alias is defined on device publish position information
         * otherwhise publish full positionData
         * this make easy to parse specific device location
         */
        final String alias = positionData.getDevice().getString(MQTT_ALIAS, "");

		if (StringUtils.isNotBlank(alias) && 
				(options.isEmpty() || options.contains(OPTION_ALIAS)) && 
				!options.contains(OPTION_NOALIAS)) {
            publishPosition(topic + "/" + alias , positionData.getPosition(), resultHandler, options);
        }

        if (options.contains(OPTION_DEVICE_ID)) {
            publishPosition(topic + "/" + positionData.getPosition().getDeviceId() , positionData.getPosition(), resultHandler, options);
        }
        
        if (StringUtils.isBlank(alias) || options.contains(OPTION_COMBINED)) {
            publish(topic, positionData, resultHandler);
        }

    	
    }

	private void publishPosition(final String topic, final Position position,  
			final ResultHandler resultHandler,
			final Set<String> options) {
		// TODO option to split position attributes in specific topics
		// for now just publish single topic with json
//		if (options.isEmpty() || options.contains(OPTION_DEVICE_JSON)) {
	        publish(topic, position, resultHandler);
//		}
	}
	
	private void publish(final String pubTopic, final Object object,  final ResultHandler resultHandler) {
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            resultHandler.onResult(false, e);
            return;
        }
		MqttUtil.publish(client, pubTopic, payload, 
				(message, e) -> resultHandler.onResult(e == null, e));
	}

}
