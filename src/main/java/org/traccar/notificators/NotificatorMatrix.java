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
package org.traccar.notificators;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.MessageException;
import org.traccar.notification.NotificationFormatter;
import org.traccar.notification.NotificationMessage;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class NotificatorMatrix extends Notificator {

    private static final String USER_ATTRIBUTE = "matrixUser";

    private final Client client;
    private final String homeserverUrl;
    private final String accessToken;
    private final Map<String, String> userRooms = new ConcurrentHashMap<>();

    public static class JoinedRoomsResponse {
        @JsonProperty("joined_rooms")
        private List<String> joinedRooms = List.of();
    }

    public static class JoinedMembersResponse {
        @JsonProperty("joined")
        private Map<String, Object> joined = Map.of();
    }

    public static class CreateRoomRequest {
        @JsonProperty("preset")
        private String preset = "trusted_private_chat";
        @JsonProperty("is_direct")
        private boolean direct = true;
        @JsonProperty("invite")
        private List<String> invite;
    }

    public static class CreateRoomResponse {
        @JsonProperty("room_id")
        private String roomId;
    }

    public static class Message {
        @JsonProperty("msgtype")
        private String messageType = "m.text";
        @JsonProperty("body")
        private String body;
    }

    @Inject
    public NotificatorMatrix(Config config, NotificationFormatter notificationFormatter, Client client) {
        super(notificationFormatter);
        this.client = client;
        homeserverUrl = normalize(config.getString(Keys.NOTIFICATOR_MATRIX_HOMESERVER_URL));
        accessToken = config.getString(Keys.NOTIFICATOR_MATRIX_ACCESS_TOKEN);
    }

    @Override
    public void send(User user, NotificationMessage shortMessage, Event event, Position position)
            throws MessageException {

        String matrixUser = getMatrixUser(user);
        if (matrixUser == null) {
            return;
        }

        String roomId = resolveRoomId(matrixUser);

        Message message = new Message();
        message.body = shortMessage.digest();

        try (Response response = matrixTarget()
                .path("rooms")
                .path(roomId)
                .path("send")
                .path("m.room.message")
                .path(UUID.randomUUID().toString())
                .request()
                .header("Authorization", "Bearer " + accessToken)
                .put(Entity.json(message))) {
            checkResponse(response);
        }
    }

    private String resolveRoomId(String matrixUser) throws MessageException {
        String cachedRoomId = userRooms.get(matrixUser);
        if (cachedRoomId != null) {
            return cachedRoomId;
        }

        String roomId = findUserRoom(matrixUser);
        if (roomId == null) {
            roomId = createRoom(matrixUser);
        }
        userRooms.put(matrixUser, roomId);
        return roomId;
    }

    private String findUserRoom(String matrixUser) throws MessageException {
        JoinedRoomsResponse joinedRooms = request(matrixTarget().path("joined_rooms"), JoinedRoomsResponse.class);
        for (String roomId : joinedRooms.joinedRooms) {
            JoinedMembersResponse members = request(
                    matrixTarget().path("rooms").path(roomId).path("joined_members"), JoinedMembersResponse.class);
            if (members.joined.containsKey(matrixUser)) {
                return roomId;
            }
        }
        return null;
    }

    private String createRoom(String matrixUser) throws MessageException {
        CreateRoomRequest request = new CreateRoomRequest();
        request.invite = List.of(matrixUser);

        CreateRoomResponse response = post(matrixTarget().path("createRoom"), request, CreateRoomResponse.class);
        if (response.roomId == null) {
            throw new MessageException("Matrix room creation failed");
        }
        return response.roomId;
    }

    private <T> T request(WebTarget target, Class<T> type) throws MessageException {
        try (Response response = target.request()
                .header("Authorization", "Bearer " + accessToken)
                .get()) {
            checkResponse(response);
            return response.readEntity(type);
        }
    }

    private <T> T post(WebTarget target, Object body, Class<T> type) throws MessageException {
        try (Response response = target.request()
                .header("Authorization", "Bearer " + accessToken)
                .post(Entity.json(body))) {
            checkResponse(response);
            return response.readEntity(type);
        }
    }

    private void checkResponse(Response response) throws MessageException {
        if (response.getStatus() / 100 != 2) {
            throw new MessageException(response.readEntity(String.class));
        }
    }

    private WebTarget matrixTarget() {
        return client.target(homeserverUrl).path("_matrix").path("client").path("v3");
    }

    private String getMatrixUser(User user) {
        if (user == null) {
            return null;
        }
        return normalize(user.getString(USER_ATTRIBUTE));
    }

    private static String normalize(String value) {
        if (value != null) {
            value = value.trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

}
