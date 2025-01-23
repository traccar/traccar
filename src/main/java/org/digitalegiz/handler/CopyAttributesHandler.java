/*
 * Copyright 2016 - 2024 Anton Tananaev (anton@digitalegiz.org)
 * Copyright 2016 - 2017 Andrey Kunitsyn (andrey@digitalegiz.org)
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
package org.digitalegiz.handler;

import jakarta.inject.Inject;
import org.digitalegiz.config.Config;
import org.digitalegiz.config.Keys;
import org.digitalegiz.helper.model.AttributeUtil;
import org.digitalegiz.model.Position;
import org.digitalegiz.session.cache.CacheManager;

public class CopyAttributesHandler extends BasePositionHandler {

    private final CacheManager cacheManager;

    @Inject
    public CopyAttributesHandler(Config config, CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        String attributesString = AttributeUtil.lookup(
                cacheManager, Keys.PROCESSING_COPY_ATTRIBUTES, position.getDeviceId());
        Position last = cacheManager.getPosition(position.getDeviceId());
        if (last != null && attributesString != null) {
            for (String attribute : attributesString.split("[ ,]")) {
                if (last.hasAttribute(attribute) && !position.hasAttribute(attribute)) {
                    position.getAttributes().put(attribute, last.getAttributes().get(attribute));
                }
            }
        }
        callback.processed(false);
    }

}
