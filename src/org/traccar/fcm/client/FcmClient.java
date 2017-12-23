// Copyright (c) Philipp Wagner. All rights reserved.

// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.client;

import org.traccar.fcm.client.http.IHttpClient;
import org.traccar.fcm.client.http.apache.DefaultHttpClient;
import org.traccar.fcm.client.settings.PropertiesBasedSettings;
import org.traccar.fcm.core.http.client.IFcmClient;
import org.traccar.fcm.core.http.options.IFcmClientSettings;
import org.traccar.fcm.core.requests.data.DataMulticastMessage;
import org.traccar.fcm.core.requests.data.DataUnicastMessage;
import org.traccar.fcm.core.requests.groups.AddDeviceGroupMessage;
import org.traccar.fcm.core.requests.groups.CreateDeviceGroupMessage;
import org.traccar.fcm.core.requests.groups.RemoveDeviceGroupMessage;
import org.traccar.fcm.core.requests.notification.NotificationMulticastMessage;
import org.traccar.fcm.core.requests.notification.NotificationUnicastMessage;
import org.traccar.fcm.core.requests.topic.TopicMulticastMessage;
import org.traccar.fcm.core.requests.topic.TopicUnicastMessage;
import org.traccar.fcm.core.responses.CreateDeviceGroupMessageResponse;
import org.traccar.fcm.core.responses.FcmMessageResponse;
import org.traccar.fcm.core.responses.TopicMessageResponse;

import java.util.Properties;

public class FcmClient implements IFcmClient {

    private final IFcmClientSettings settings;
    private final IHttpClient httpClient;

    public FcmClient(Properties properties) {
        this(new PropertiesBasedSettings(properties));
    }

    public FcmClient(IFcmClientSettings settings) {
        this(settings, new DefaultHttpClient(settings));
    }

    public FcmClient(IFcmClientSettings settings, IHttpClient httpClient) {

        if (settings == null) {
            throw new IllegalArgumentException("settings");
        }

        if (httpClient == null) {
            throw new IllegalArgumentException("httpClient");
        }

        this.settings = settings;
        this.httpClient = httpClient;
    }

    @Override
    public FcmMessageResponse send(DataMulticastMessage message) {
        return post(message, FcmMessageResponse.class);
    }

    @Override
    public FcmMessageResponse send(NotificationMulticastMessage notification) {
        return post(notification, FcmMessageResponse.class);
    }

    @Override
    public FcmMessageResponse send(DataUnicastMessage message) {
        return post(message, FcmMessageResponse.class);
    }

    @Override
    public FcmMessageResponse send(NotificationUnicastMessage notification) {
        return post(notification, FcmMessageResponse.class);
    }

    @Override
    public CreateDeviceGroupMessageResponse send(CreateDeviceGroupMessage message) {
        return post(message, CreateDeviceGroupMessageResponse.class);
    }

    @Override
    public TopicMessageResponse send(TopicUnicastMessage message) {
        return post(message, TopicMessageResponse.class);
    }

    @Override
    public TopicMessageResponse send(TopicMulticastMessage message) {
        return post(message, TopicMessageResponse.class);
    }

    @Override
    public void send(RemoveDeviceGroupMessage message) {
        post(message);
    }

    @Override
    public void send(AddDeviceGroupMessage message) {
        post(message);
    }

    protected <TRequestMessage, TResponseMessage> TResponseMessage post(
            TRequestMessage requestMessage, Class<TResponseMessage> responseType) {
        return httpClient.post(requestMessage, responseType);
    }

    protected <TRequestMessage> void post(TRequestMessage requestMessage) {
        httpClient.post(requestMessage);
    }

    @Override
    public void close() throws Exception {
        httpClient.close();
    }
}