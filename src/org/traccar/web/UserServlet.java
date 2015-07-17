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
import org.traccar.model.User;

public class UserServlet extends BaseServlet {

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
        Context.getPermissionsManager().checkAdmin(getUserId(req));
        sendResponse(resp.getWriter(), JsonConverter.arrayToJson(
                    Context.getDataManager().getUsers()));
    }
    
    private void add(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = JsonConverter.objectFromJson(req.getReader(), new User());
        Context.getPermissionsManager().checkUser(getUserId(req), user.getId());
        Context.getDataManager().addUser(user);
        Context.getPermissionsManager().refresh();
        sendResponse(resp.getWriter(), JsonConverter.objectToJson(user));
    }
    
    private void update(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = JsonConverter.objectFromJson(req.getReader(), new User());
        if (user.getAdmin()) {
            Context.getPermissionsManager().checkAdmin(getUserId(req));
        } else {
            Context.getPermissionsManager().checkUser(getUserId(req), user.getId());
        }
        Context.getDataManager().updateUser(user);
        Context.getPermissionsManager().refresh();
        sendResponse(resp.getWriter(), true);
    }
    
    private void remove(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = JsonConverter.objectFromJson(req.getReader(), new User());
        Context.getPermissionsManager().checkUser(getUserId(req), user.getId());
        Context.getDataManager().removeUser(user);
        Context.getPermissionsManager().refresh();
        sendResponse(resp.getWriter(), true);
    }

}
