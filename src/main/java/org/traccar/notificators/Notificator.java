/*
 * Copyright 2018 - 2026 Anton Tananaev (anton@traccar.org)
 * Copyright 2018 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.notificators;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.Response;
import org.traccar.model.Event;
import org.traccar.model.Notification;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.NotificationFormatter;
import org.traccar.notification.NotificationMessage;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class Notificator {

    private final NotificationFormatter notificationFormatter;

    public Notificator(NotificationFormatter notificationFormatter) {
        this.notificationFormatter = notificationFormatter;
    }

    public CompletableFuture<Void> sendAsync(Notification notification, User user, Event event, Position position) {
        try {
            var message = notificationFormatter.formatMessage(notification, user, event, position);
            return sendAsync(user, message, event, position);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Void> sendAsync(User user, NotificationMessage message, Event event, Position position) {
        throw new UnsupportedOperationException();
    }

    protected static CompletableFuture<Void> post(
            Invocation.Builder request, Entity<?> entity, Consumer<Response> handler) {
        var future = new CompletableFuture<Void>();
        request.async().post(entity, new InvocationCallback<Response>() {
            @Override
            public void completed(Response response) {
                try (response) {
                    handler.accept(response);
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }

            @Override
            public void failed(Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

}
