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
package org.traccar.handler.events;

import io.netty.channel.ChannelHandler;
import org.traccar.model.Event;
import org.traccar.model.Position;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@ChannelHandler.Sharable
public class MediaEventHandler extends BaseEventHandler {

    @Inject
    public MediaEventHandler() {
    }

    @Override
    protected Map<Event, Position> analyzePosition(Position position) {
        return Stream.of(Position.KEY_IMAGE, Position.KEY_VIDEO, Position.KEY_AUDIO)
                .filter(position::hasAttribute)
                .map(type -> {
                    Event event = new Event(Event.TYPE_MEDIA, position);
                    event.set("media", type);
                    event.set("file", position.getString(type));
                    return event;
                })
                .collect(Collectors.toMap(event -> event, event -> position));
    }

}
