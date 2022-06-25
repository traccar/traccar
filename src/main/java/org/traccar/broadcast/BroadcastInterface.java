/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.broadcast;

import org.traccar.model.BaseModel;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

public interface BroadcastInterface {

    default void updateDevice(boolean local, Device device) {
    }

    default void updatePosition(boolean local, Position position) {
    }

    default void updateEvent(boolean local, long userId, Event event) {
    }

    default void invalidateObject(Class<? extends BaseModel> clazz, long id) {
    }

    default void invalidatePermission(
            Class<? extends BaseModel> clazz1, long id1,
            Class<? extends BaseModel> clazz2, long id2) {
    }
}
