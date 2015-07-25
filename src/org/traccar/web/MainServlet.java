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

public class MainServlet extends BaseServlet {

    @Override
    protected boolean handle(String command, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        
        switch (command) {
            case "/session":
                session(req, resp);
                break;
            case "/login":
                login(req, resp);
                break;
            case "/logout":
                logout(req, resp);
                break;
            case "/register":
                register(req, resp);
                break;
            default:
                return false;
        }
        return true;        
    }

    private void session(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Long userId = (Long) req.getSession().getAttribute(USER_KEY);
        if (userId != null) {
            sendResponse(resp.getWriter(), JsonConverter.objectToJson(
                    Context.getDataManager().getUser(userId)));
        } else {
            sendResponse(resp.getWriter(), false);
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = Context.getDataManager().login(
                req.getParameter("email"), req.getParameter("password"));
        if (user != null) {
            req.getSession().setAttribute(USER_KEY, user.getId());
            sendResponse(resp.getWriter(), JsonConverter.objectToJson(user));
        } else {
            sendResponse(resp.getWriter(), false);
        }
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        req.getSession().removeAttribute(USER_KEY);
        sendResponse(resp.getWriter(), true);
    }

    private void register(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = JsonConverter.objectFromJson(req.getReader(), new User());
        Context.getDataManager().addUser(user);
        sendResponse(resp.getWriter(), true);
    }

}
