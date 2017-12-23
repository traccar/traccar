// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.requests.topic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.traccar.fcm.core.model.options.FcmMessageOptions;
import org.traccar.fcm.core.model.topics.Topic;
import org.traccar.fcm.core.requests.FcmUnicastMessage;
import org.traccar.fcm.core.requests.notification.NotificationPayload;

public class TopicUnicastMessage extends FcmUnicastMessage<Object> {

    private final Object data;
    private final NotificationPayload notification;

    public TopicUnicastMessage(FcmMessageOptions options, Topic to, Object data) {
        this(options, to, data, null);
    }

    public TopicUnicastMessage(FcmMessageOptions options, Topic to, NotificationPayload notification) {
        this(options, to, null, notification);
    }

    public TopicUnicastMessage(FcmMessageOptions options, Topic to, Object data, NotificationPayload notification) {
        super(options, to.getTopicPath());

        this.data = data;
        this.notification = notification;
    }

    @Override
    @JsonProperty("data")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Object getPayload() {
        return data;
    }

    @JsonProperty("notification")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public NotificationPayload getNotificationPayload() {
        return this.notification;
    }
}
