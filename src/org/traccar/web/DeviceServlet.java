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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.traccar.Context;
import org.traccar.model.Device;

public class DeviceServlet extends BaseServlet {

    @Override
    protected boolean handle(String command, HttpServletRequest req, HttpServletResponse resp) throws Exception {

        switch (command) {
            case "/get":
                get(req, resp);
                break;
            case "/add":
                add(req, resp);
                break;
            case "/update":
                update(req, resp);
                break;
            case "/remove":
                remove(req, resp);
                break;
            default:
                return false;
        }
        return true;
    }
    
    private void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        sendResponse(resp.getWriter(), JsonConverter.arrayToJson(
                    Context.getDataManager().getDevices(getUserId(req))));
    }
    
    private void add(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Device device = JsonConverter.objectFromJson(req.getReader(), new Device());
        long userId = getUserId(req);
        Context.getDataManager().addDevice(device);
        Context.getDataManager().linkDevice(userId, device.getId());
        Context.getPermissionsManager().refresh();
        sendResponse(resp.getWriter(), JsonConverter.objectToJson(device));
    }
    
    private void update(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Device device = JsonConverter.objectFromJson(req.getReader(), new Device());
        Context.getPermissionsManager().checkDevice(getUserId(req), device.getId());
        Context.getDataManager().updateDevice(device);
        sendResponse(resp.getWriter(), true);
    }
    
    private void remove(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Device device = JsonConverter.objectFromJson(req.getReader(), new Device());
        Context.getPermissionsManager().checkDevice(getUserId(req), device.getId());
        Context.getDataManager().removeDevice(device);
        Context.getPermissionsManager().refresh();
        sendResponse(resp.getWriter(), true);
    }

}
