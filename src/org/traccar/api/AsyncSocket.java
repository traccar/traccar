/*
 * Copyright 2015 - 2016 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.api;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.traccar.Context;
import org.traccar.database.ConnectionManager;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.web.JsonConverter;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.Collection;
import java.util.Collections;

public class AsyncSocket extends WebSocketAdapter implements ConnectionManager.UpdateListener {

    private static final String KEY_DEVICES = "devices";
    private static final String KEY_POSITIONS = "positions";

    private long userId;

    public AsyncSocket(long userId) {
        this.userId = userId;
    }

    @Override
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);

        sendData(KEY_POSITIONS, Context.getConnectionManager().getInitialState(userId));

        Context.getConnectionManager().addListener(userId, this);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);

        Context.getConnectionManager().removeListener(userId, this);
    }

    @Override
    public void onUpdateDevice(Device device) {
        sendData(KEY_DEVICES, Collections.singletonList(device));
    }

    @Override
    public void onUpdatePosition(Position position) {
        sendData(KEY_POSITIONS, Collections.singletonList(position));
    }

    private void sendData(String key, Collection<?> data) {
        if (!data.isEmpty() && isConnected()) {
            JsonObjectBuilder json = Json.createObjectBuilder();
            json.add(key, JsonConverter.arrayToJson(data));
            getRemote().sendString(json.build().toString(), null);
        }
    }

}
