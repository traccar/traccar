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
import org.traccar.helper.CommandCall;
import org.traccar.model.User;

public class UserServlet extends BaseServletResource<User> {

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

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Context.getPermissionsManager().checkAdmin(getUserId(req));
        sendResponse(resp.getWriter(), JsonConverter.arrayToJson(
                    Context.getDataManager().getUsers()));
    }

    @Override
    protected void add(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        super.add(req, resp, new CommandCall<User>() {

            @Override
            public void check() throws Exception {
                Context.getPermissionsManager().check(User.class, getUserId(), getEntity().getId());
            }

            @Override
            public void after() throws Exception {
                Context.getPermissionsManager().refresh();
            }
        });
    }

    @Override
    protected void update(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        super.update(req, resp, new CommandCall<User>() {

            @Override
            public void check() {
                if (getEntity().getAdmin()) {
                    Context.getPermissionsManager().checkAdmin(getUserId());
                } else {
                    Context.getPermissionsManager().check(User.class, getUserId(), getEntity().getId());
                }
            }

            @Override
            public void after() {
                Context.getPermissionsManager().refresh();
            }

        });
    }

    @Override
    protected void remove(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        super.remove(req, resp, new CommandCall<User>() {

            @Override
            public void check() throws Exception {
                Context.getPermissionsManager().check(User.class, getUserId(), getEntity().getId());
            }

            @Override
            public void after() throws Exception {
                Context.getPermissionsManager().refresh();
            }

        });
    }
}
