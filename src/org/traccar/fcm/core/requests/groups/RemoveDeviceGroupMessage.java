// Copyright (c) Philipp Wagner. All rights reserved.

// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.requests.groups;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.traccar.fcm.core.model.enums.OperationEnum;
import org.traccar.fcm.core.model.options.FcmMessageOptions;

import java.util.List;

public class RemoveDeviceGroupMessage extends DeviceGroupMessage {

    private final String notificationKey;

    public RemoveDeviceGroupMessage(FcmMessageOptions options,
                                    List<String> registrationIds, String notificationKeyName, String notificationKey) {
        super(options, registrationIds, notificationKeyName);

        this.notificationKey = notificationKey;
    }

    @Override
    public OperationEnum getOperation() {
        return OperationEnum.Remove;
    }

    @JsonProperty("notification_key")
    public String getNotificationKey() {
        return this.notificationKey;
    }

}
