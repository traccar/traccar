// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.requests.groups;

import org.traccar.fcm.core.model.enums.OperationEnum;
import org.traccar.fcm.core.model.options.FcmMessageOptions;

import java.util.List;

public class CreateDeviceGroupMessage extends DeviceGroupMessage {

    public CreateDeviceGroupMessage(FcmMessageOptions options,
                                    List<String> registrationIds, String notificationKeyName) {
        super(options, registrationIds, notificationKeyName);
    }

    @Override
    public OperationEnum getOperation() {
        return OperationEnum.Create;
    }

}
