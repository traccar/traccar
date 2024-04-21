/*
 * Copyright 2016 - 2024 Anton Tananaev (anton@traccar.org)
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
package org.traccar.handler.events;

import org.traccar.model.Event;
import org.traccar.model.Position;

public abstract class BaseEventHandler {

    public interface Callback {
        void eventDetected(Event event);
    }

    /**
     * Event handlers should be processed synchronously.
     */
    public abstract void analyzePosition(Position position, Callback callback);
}
