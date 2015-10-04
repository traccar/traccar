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

import org.traccar.helper.Log;

import java.io.IOException;
import java.io.Writer;
import java.security.AccessControlException;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class BaseServlet extends HttpServlet {

    public static final String USER_KEY = "user";

    @Override
    protected final void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String command = req.getPathInfo();
        if (command == null) {
            command = "";
        }
        try {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Access-Control-Allow-Origin", "*");
            resp.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
            resp.setHeader("Access-Control-Allow-Methods", "GET, POST");
            if (!handle(command, req, resp)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception error) {
            sendResponse(resp.getWriter(), error);
        }
    }

    protected abstract boolean handle(String command, HttpServletRequest req, HttpServletResponse resp) throws Exception;

    public long getUserId(HttpServletRequest req) {
        Long userId = (Long) req.getSession().getAttribute(USER_KEY);
        if (userId == null) {
            throw new AccessControlException("User not logged in");
        }
        return userId;
    }

    public void sendResponse(Writer writer, boolean success) throws IOException {
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("success", success);
        writer.write(result.build().toString());
    }

    public void sendResponse(Writer writer, JsonStructure json) throws IOException {
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("success", true);
        result.add("data", json);
        writer.write(result.build().toString());
    }

    public void sendResponse(Writer writer, Exception error) throws IOException {
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("success", false);
        result.add("error", Log.exceptionStack(error));
        writer.write(result.build().toString());
    }

}
