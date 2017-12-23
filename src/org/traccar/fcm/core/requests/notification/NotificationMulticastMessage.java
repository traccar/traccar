// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.requests.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.traccar.fcm.core.model.options.FcmMessageOptions;
import org.traccar.fcm.core.requests.FcmMulticastMessage;

import java.util.List;

public class NotificationMulticastMessage extends FcmMulticastMessage<NotificationPayload> {

    private final NotificationPayload notificationPayload;

    public NotificationMulticastMessage(FcmMessageOptions options, List<String> registrationIds, NotificationPayload notificationPayload) {
        super(options, registrationIds);

        if (notificationPayload == null) {
            throw new IllegalArgumentException("notificationPayload");
        }

        this.notificationPayload = notificationPayload;
    }

    @Override
    @JsonProperty("notification")
    public NotificationPayload getPayload() {
        return this.notificationPayload;
    }
}
