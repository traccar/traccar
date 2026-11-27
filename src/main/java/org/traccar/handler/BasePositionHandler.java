/*
 * Copyright 2015 - 2024 Anton Tananaev (anton@traccar.org)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.Position;

public abstract class BasePositionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePositionHandler.class);

    public interface Callback {
        void processed(boolean filtered);
    }

    public abstract void onPosition(Position position, Callback callback);

    public void handlePosition(Position position, Callback callback) {
        try {
            onPosition(position, callback);
        } catch (RuntimeException e) {
            LOGGER.warn("Position handler failed", e);
            callback.processed(false);
        }
    }
}
