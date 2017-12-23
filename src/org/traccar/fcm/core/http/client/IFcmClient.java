// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.http.client;

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

public interface IFcmClient extends AutoCloseable {

    FcmMessageResponse send(DataMulticastMessage message);

    FcmMessageResponse send(NotificationMulticastMessage notification);

    FcmMessageResponse send(DataUnicastMessage message);

    FcmMessageResponse send(NotificationUnicastMessage notification);

    CreateDeviceGroupMessageResponse send(CreateDeviceGroupMessage message);

    TopicMessageResponse send(TopicUnicastMessage message);

    TopicMessageResponse send(TopicMulticastMessage message);

    void send(RemoveDeviceGroupMessage message);

    void send(AddDeviceGroupMessage message);

}
