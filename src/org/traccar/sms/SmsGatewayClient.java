/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
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
package org.traccar.sms;

import javax.json.Json;
import javax.json.JsonArray;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.traccar.Context;
import org.traccar.notification.MessageException;

public class SmsGatewayClient implements SmsManager {

    private static final String URL = "https://smsgateway.me/api/v4/message/send";
    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_AUTHORIZATION = "Authorization";

    private final String token;
    private final int deviceId;

    public SmsGatewayClient() {
        token = Context.getConfig().getString("sms.smsgateway.token");
        deviceId = Context.getConfig().getInteger("sms.smsgateway.deviceId");
    }

    private JsonArray preparePayload(String destAddress, String message) {
        return Json.createArrayBuilder()
            .add(Json.createObjectBuilder()
                .add(KEY_PHONE_NUMBER, destAddress)
                .add(KEY_MESSAGE, message)
                .add(KEY_DEVICE_ID, deviceId))
            .build();
    }

    private Invocation.Builder getRequestBuilder() {
        return Context.getClient().target(URL).request()
                .header(KEY_AUTHORIZATION, token);
    }

    @Override
    public void sendMessageSync(String destAddress, String message, boolean command)
        throws InterruptedException, MessageException {
        Response response = getRequestBuilder().post(Entity.json(preparePayload(destAddress, message)));
        if (!response.getStatusInfo().equals(Response.Status.OK)) {
            String output = response.readEntity(String.class);
            throw new MessageException(output);
        }
    }

    @Override
    public void sendMessageAsync(final String destAddress, final String message, final boolean command) {
        getRequestBuilder().async().post(Entity.json(preparePayload(destAddress, message)));
    }
}
