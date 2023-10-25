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
package org.traccar.helper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.auth.Mqtt3SimpleAuth;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.nimbusds.oauth2.sdk.util.StringUtils;

/**
 * MQTT generic calls 
 * 
 */
public class MqttUtil {

    /**
     * Create client from url
     * 
     * @param url
     * @return
     */
    public static Mqtt3AsyncClient createClient(final String url, final String willTopic) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        final Mqtt3SimpleAuth simpleAuth = getSimpleAuth(uri);

        final String host = uri.getHost();
        final int port = uri.getPort();
        final Mqtt3ClientBuilder builder = Mqtt3Client.builder()
                .identifier("traccar-" + UUID.randomUUID())
                .serverHost(host)
                .serverPort(port)
                .simpleAuth(simpleAuth)
                .automaticReconnectWithDefaultConfig();
    	final Mqtt3AsyncClient client;
        if (StringUtils.isNotBlank(willTopic)) {
        	client = builder.willPublish()
        				.topic(willTopic)
        				.payload("disconnected".getBytes())
        				.retain(true)
        				.applyWillPublish()
        				.buildAsync();
        } else {
        	client = builder.buildAsync(); 
        }

        client.connectWith()
                .send()
                .whenComplete((message, e) -> {
                    if (e != null) {
                        throw new RuntimeException(e);
                    } else if (StringUtils.isNotBlank(willTopic)) {
                    	client.publishWith()
                    		.topic(willTopic)
                    		.payload("connected".getBytes())
                    		.retain(true)
                    		.send();
                    }
                });

        return client;
    }

	private static Mqtt3SimpleAuth getSimpleAuth(final URI uri) {
		final String userInfo = uri.getUserInfo();
        Mqtt3SimpleAuth simpleAuth = null;
        if (userInfo != null) {
            int delimiter = userInfo.indexOf(':');
            if (delimiter == -1) {
                throw new IllegalArgumentException("Wrong MQTT credentials. Should be in format \"username:password\"");
            } else {
                simpleAuth = Mqtt3SimpleAuth.builder()
                        .username(userInfo.substring(0, delimiter++))
                        .password(userInfo.substring(delimiter).getBytes())
                        .build();
            }
        }
		return simpleAuth;
	}

	public static void publish(final Mqtt3AsyncClient client, 
			final String pubTopic, final String payload,
			final BiConsumer<? super Mqtt3Publish, ? super Throwable> whenComplete) {
		client.publishWith()
        	.topic(pubTopic)
        	.qos(MqttQos.AT_LEAST_ONCE)
        	.payload(payload.getBytes())
        	.send()
        	.whenComplete(whenComplete);
	}

}
