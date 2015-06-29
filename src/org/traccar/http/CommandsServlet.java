package org.traccar.http;

import org.traccar.Context;
import org.traccar.database.ActiveDevice;
import org.traccar.command.CommandType;
import org.traccar.command.GpsCommand;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CommandsServlet extends BaseServlet {

    @Override
    protected boolean handle(String command, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (command.equals("/send")) {
            send(req, resp);
        }
        else if (command.equals("/raw")) {
            sendRawCommand(req, resp);
        }
        else {
            return false;
        }
        return true;
    }

    private void send(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        GpsCommand command = JsonConverter.<GpsCommand>enumObjectFromJson(req.getReader(), new EnumFactory(CommandType.class, "type"));

        String uniqueId = command.getUniqueId();

        ActiveDevice activeDevice = Context.getConnectionManager().getActiveDevice(uniqueId);
        if(activeDevice == null) {
            throw new RuntimeException("The device has not yet registered to the server");
        }

        activeDevice.sendCommand(command);

        sendResponse(resp.getWriter(), JsonConverter.objectToJson(new Object()));
    }

    private void sendRawCommand(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonObject json = Json.createReader(req.getReader()).readObject();
        String uniqueId = json.getString("uniqueId");

        ActiveDevice activeDevice = Context.getConnectionManager().getActiveDevice(uniqueId);
        if(activeDevice == null) {
            throw new RuntimeException("The device has not yet registered to the server");
        }

        String command = json.getString("command");
        activeDevice.write(command);

        sendResponse(resp.getWriter(), JsonConverter.objectToJson(new Object()));
    }
}
