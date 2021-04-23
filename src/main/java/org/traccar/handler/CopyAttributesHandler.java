/*
 * Copyright 2016 - 2019 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 - 2017 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.handler;

import io.netty.channel.ChannelHandler;
import org.traccar.BaseDataHandler;
import org.traccar.database.IdentityManager;
import org.traccar.model.Position;

@ChannelHandler.Sharable
public class CopyAttributesHandler extends BaseDataHandler {

    private IdentityManager identityManager;

    public CopyAttributesHandler(IdentityManager identityManager) {
        this.identityManager = identityManager;
    }

    @Override
    protected Position handlePosition(Position position) {
        String attributesString = identityManager.lookupAttributeString(
                position.getDeviceId(), "processing.copyAttributes", "", false, true);
        Position last = identityManager.getLastPosition(position.getDeviceId());
        if (last != null) {
            for (String attribute : attributesString.split("[ ,]")) {
                if (last.getAttributes().containsKey(attribute) && !position.getAttributes().containsKey(attribute)) {
                    position.getAttributes().put(attribute, last.getAttributes().get(attribute));
                }
            }
        }
        return position;
    }

}
