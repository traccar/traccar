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
import org.traccar.model.Server;

public class ServerServlet extends BaseServlet {

    @Override
    protected boolean handle(String command, HttpServletRequest req, HttpServletResponse resp) throws Exception {

        switch (command) {
            case "/get":
                get(resp);
                break;
            case "/update":
                update(req, resp);
                break;
            default:
                return false;
        }
        return true;
    }

    private void get(HttpServletResponse resp) throws Exception {
        sendResponse(resp.getWriter(), JsonConverter.objectToJson(
                    Context.getDataManager().getServer()));
    }

    private void update(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Server server = JsonConverter.objectFromJson(req.getReader(), new Server());
        Context.getPermissionsManager().checkAdmin(getUserId(req));
        Context.getDataManager().updateServer(server);
        sendResponse(resp.getWriter(), true);
    }

}
