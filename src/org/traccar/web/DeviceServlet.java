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
            case "/link":
                link(req, resp);
                break;
            case "/unlink":
                unlink(req, resp);
                break;
            default:
                return false;
        }
        return true;
    }

    private void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (Boolean.parseBoolean(req.getParameter("all"))) {
            Context.getPermissionsManager().checkAdmin(getUserId(req));
            sendResponse(resp.getWriter(), JsonConverter.arrayToJson(
                    Context.getDataManager().getAllDevices()));
        } else {
            long userId;
            String userIdParam = req.getParameter("userId");
            if (userIdParam != null) {
                userId = Long.parseLong(userIdParam);
            } else {
                userId = getUserId(req);
            }
            Context.getPermissionsManager().checkUser(getUserId(req), userId);
            sendResponse(resp.getWriter(), JsonConverter.arrayToJson(
                    Context.getDataManager().getDevices(userId)));
        }
    }

    private void add(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Device device = JsonConverter.objectFromJson(req.getReader(), Device.class);
        long userId = getUserId(req);
        Context.getDataManager().addDevice(device);
        Context.getDataManager().linkDevice(userId, device.getId());
        Context.getPermissionsManager().refresh();
        sendResponse(resp.getWriter(), JsonConverter.objectToJson(device));
    }

    private void update(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Device device = JsonConverter.objectFromJson(req.getReader(), Device.class);
        Context.getPermissionsManager().checkDevice(getUserId(req), device.getId());
        Context.getDataManager().updateDevice(device);
        sendResponse(resp.getWriter(), true);
    }

    private void remove(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Device device = JsonConverter.objectFromJson(req.getReader(), Device.class);
        Context.getPermissionsManager().checkDevice(getUserId(req), device.getId());
        Context.getDataManager().removeDevice(device);
        Context.getPermissionsManager().refresh();
        sendResponse(resp.getWriter(), true);
    }

    private void link(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Context.getPermissionsManager().checkAdmin(getUserId(req));
        Context.getDataManager().linkDevice(
                Long.parseLong(req.getParameter("userId")),
                Long.parseLong(req.getParameter("deviceId")));
        Context.getPermissionsManager().refresh();
        sendResponse(resp.getWriter(), true);
    }

    private void unlink(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Context.getPermissionsManager().checkAdmin(getUserId(req));
        Context.getDataManager().unlinkDevice(
                Long.parseLong(req.getParameter("userId")),
                Long.parseLong(req.getParameter("deviceId")));
        Context.getPermissionsManager().refresh();
        sendResponse(resp.getWriter(), true);
    }

}
