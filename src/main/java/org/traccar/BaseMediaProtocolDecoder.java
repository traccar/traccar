/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
package org.traccar;

import io.netty.channel.Channel;
import org.traccar.database.DeviceLookupService;
import org.traccar.model.Device;
import org.traccar.session.DeviceSession;

import jakarta.inject.Inject;
import java.net.SocketAddress;

public abstract class BaseMediaProtocolDecoder extends BaseProtocolDecoder {

    private final Protocol protocol;
    private DeviceLookupService deviceLookupService;
    private DeviceSession deviceSession;

    public BaseMediaProtocolDecoder(Protocol protocol) {
        super(protocol);
        this.protocol = protocol;
    }

    @Inject
    public void setDeviceLookupService(DeviceLookupService deviceLookupService) {
        this.deviceLookupService = deviceLookupService;
    }

    @Override
    public DeviceSession getDeviceSession(Channel channel, SocketAddress remoteAddress, String... uniqueIds) {
        if (uniqueIds.length > 0) {
            deviceSession = null;
            Device device = deviceLookupService.lookup(uniqueIds);
            if (device != null) {
                device.checkDisabled();
                deviceSession = new DeviceSession(
                        device.getId(), device.getUniqueId(), device.getModel(), protocol, channel, remoteAddress);
            }
        }
        if (deviceSession != null) {
            deviceSession.setLastUpdate(System.currentTimeMillis());
        }
        return deviceSession;
    }

    @Override
    protected void sendQueuedCommands(Channel channel, SocketAddress remoteAddress, long deviceId) {}

}
