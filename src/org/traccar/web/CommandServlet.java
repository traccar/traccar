/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.web;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.traccar.Context;
import org.traccar.database.ActiveDevice;
import org.traccar.model.Command;
import org.traccar.model.Device;

public class CommandServlet extends BaseServlet {

    @Override
    protected boolean handle(String command, HttpServletRequest req, HttpServletResponse resp) throws Exception {

        switch (command) {
            case "/send":
                send(req, resp);
                return true;
            case "/raw":
                raw(req, resp);
                return true;
            default:
                return false;
        }
    }

    public ActiveDevice getActiveDevice(long deviceId) {
        ActiveDevice activeDevice = Context.getConnectionManager().getActiveDevice(deviceId);
        if (activeDevice == null) {
            throw new RuntimeException("The device is not registered on the server");
        }
        return activeDevice;
    }

    private void send(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Command command = JsonConverter.objectFromJson(req.getReader(), Command.class);
        Context.getPermissionsManager().check(Device.class, getUserId(req), command.getDeviceId());
        getActiveDevice(command.getDeviceId()).sendCommand(command);
        sendResponse(resp.getWriter(), true);
    }

    private void raw(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonObject json = Json.createReader(req.getReader()).readObject();
        long deviceId = json.getJsonNumber("deviceId").longValue();
        String command = json.getString("command");
        Context.getPermissionsManager().check(Device.class, getUserId(req), deviceId);
        getActiveDevice(deviceId).write(command);
        sendResponse(resp.getWriter(), true);
    }
}
