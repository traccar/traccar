/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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
package org.traccar.handler.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class AcknowledgementHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcknowledgementHandler.class);

    public interface Event {
    }

    public static class EventReceived implements Event {
    }

    public static class EventDecoded implements Event {
        private final Collection<Object> objects;

        public EventDecoded(Collection<Object> objects) {
            this.objects = objects;
        }

        public Collection<Object> getObjects() {
            return objects;
        }
    }

    public static class EventHandled implements Event {
        private final Object object;

        public EventHandled(Object object) {
            this.object = object;
        }

        public Object getObject() {
            return object;
        }
    }

    private static final class Entry {
        private final Object message;
        private final ChannelPromise promise;

        private Entry(Object message, ChannelPromise promise) {
            this.message = message;
            this.promise = promise;
        }

        public Object getMessage() {
            return message;
        }

        public ChannelPromise getPromise() {
            return promise;
        }
    }

    private List<Entry> queue;
    private final Set<Object> waiting = new HashSet<>();

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        List<Entry> output = new LinkedList<>();
        synchronized (this) {
            if (msg instanceof Event) {
                if (msg instanceof EventReceived) {
                    LOGGER.debug("Event received");
                    if (queue == null) {
                        queue = new LinkedList<>();
                    }
                } else if (msg instanceof EventDecoded) {
                    EventDecoded event = (EventDecoded) msg;
                    LOGGER.debug("Event decoded {}", event.getObjects().size());
                    waiting.addAll(event.getObjects());
                } else if (msg instanceof EventHandled) {
                    EventHandled event = (EventHandled) msg;
                    LOGGER.debug("Event handled");
                    waiting.remove(event.getObject());
                }
                if (!(msg instanceof EventReceived) && waiting.isEmpty()) {
                    output.addAll(queue);
                    queue = null;
                }
            } else if (queue != null) {
                LOGGER.debug("Message queued");
                queue.add(new Entry(msg, promise));
            } else {
                LOGGER.debug("Message sent");
                output.add(new Entry(msg, promise));
            }
        }
        for (Entry entry : output) {
            ctx.write(entry.getMessage(), entry.getPromise());
        }
    }

}
