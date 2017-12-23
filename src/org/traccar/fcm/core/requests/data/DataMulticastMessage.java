// Copyright (c) Philipp Wagner. All rights reserved.

// Licensed under the MIT license. See LICENSE file in the project root for full license information.


package org.traccar.fcm.core.requests.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.traccar.fcm.core.model.options.FcmMessageOptions;
import org.traccar.fcm.core.requests.FcmMulticastMessage;
import org.traccar.fcm.core.requests.notification.NotificationPayload;

import java.util.List;

public class DataMulticastMessage extends FcmMulticastMessage<Object> {

    private final Object data;
    private final NotificationPayload notification;

    public DataMulticastMessage(FcmMessageOptions options, List<String> registratiodIds, Object data) {
        this(options, registratiodIds, data, null);
    }

    public DataMulticastMessage(FcmMessageOptions options,
                                List<String> registratiodIds, Object data, NotificationPayload notification) {
        super(options, registratiodIds);

        this.data = data;
        this.notification = notification;
    }

    @Override
    @JsonProperty("data")
    public Object getPayload() {
        return data;
    }

    @JsonProperty("notification")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public NotificationPayload getNotificationPayload() {
        return this.notification;
    }

}
