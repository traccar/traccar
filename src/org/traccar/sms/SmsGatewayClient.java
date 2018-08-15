/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.traccar.sms;

import javax.json.Json;
import javax.json.JsonArray;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.notification.MessageException;

/**
 *
 * @author marcos
 */
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

    protected JsonArray preparePayload(String destAddress, String message) {
        return Json.createArrayBuilder()
            .add(Json.createObjectBuilder()
                .add(KEY_PHONE_NUMBER, destAddress)
                .add(KEY_MESSAGE, message)
                .add(KEY_DEVICE_ID, deviceId))
            .build();
    }

    @Override
    public void sendMessageSync(String destAddress, String message, boolean command)
        throws InterruptedException, MessageException {
        Invocation.Builder requestBuilder = Context.getClient().target(URL).request();

        requestBuilder = requestBuilder.header(KEY_AUTHORIZATION, token);

        Response response = requestBuilder.post(Entity.json(preparePayload(destAddress, message)));
        if (!response.getStatusInfo().equals(Response.Status.OK)) {
            String output = response.readEntity(String.class);
            Log.error(output);
        }
    }

    @Override
    public void sendMessageAsync(final String destAddress, final String message, final boolean command) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    sendMessageSync(destAddress, message, command);
                } catch (MessageException | InterruptedException error) {
                    Log.warning(error);
                }
            }
        }).start();
    }
}
