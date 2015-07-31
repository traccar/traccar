package org.traccar.web;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.traccar.Context;
import org.traccar.database.ActiveDevice;
import org.traccar.model.Command;

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

        Command command = JsonConverter.objectFromJson(req.getReader(), new Command());
        Context.getPermissionsManager().checkDevice(getUserId(req), command.getDeviceId());
        getActiveDevice(command.getDeviceId()).sendCommand(command);
        sendResponse(resp.getWriter(), true);
    }

    private void raw(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        JsonObject json = Json.createReader(req.getReader()).readObject();
        long deviceId = json.getJsonNumber("deviceId").longValue();
        String command = json.getString("command");
        Context.getPermissionsManager().checkDevice(getUserId(req), deviceId);
        getActiveDevice(deviceId).write(command);
        sendResponse(resp.getWriter(), true);
    }
}
