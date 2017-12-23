// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.requests.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.traccar.fcm.core.model.options.FcmMessageOptions;
import org.traccar.fcm.core.requests.FcmUnicastMessage;

public class NotificationUnicastMessage extends FcmUnicastMessage<NotificationPayload> {

    private final NotificationPayload notificationPayload;

    public NotificationUnicastMessage(FcmMessageOptions options, String to, NotificationPayload notificationPayload) {
        super(options, to);

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
