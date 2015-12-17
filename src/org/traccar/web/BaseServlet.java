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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.AccessControlException;
import java.util.Collection;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.util.CharsetUtil;
import org.traccar.Context;

public abstract class BaseServlet extends HttpServlet {

    public static final String USER_ID_KEY = "userId";
    public static final String ALLOW_ORIGIN_VALUE = "*";
    public static final String ALLOW_HEADERS_VALUE = "Origin, X-Requested-With, Content-Type, Accept";
    public static final String ALLOW_METHODS_VALUE = "GET, POST, PUT, DELETE";
    public static final String APPLICATION_JSON = "application/json";

    @Override
    protected final void service(
            HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.setContentType(APPLICATION_JSON);
            resp.setCharacterEncoding(CharsetUtil.UTF_8.name());
            resp.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS, ALLOW_HEADERS_VALUE);
            resp.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS, ALLOW_METHODS_VALUE);

            String origin = req.getHeader(HttpHeaders.Names.ORIGIN);
            String allowed = Context.getConfig().getString("web.origin");
            if (allowed == null) {
                resp.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ORIGIN_VALUE);
            } else if (allowed.contains(origin)) {
                String originSafe = URLEncoder.encode(origin, StandardCharsets.UTF_8.name());
                resp.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, originSafe);
            }

            if (!handle(getCommand(req), req, resp)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception error) {
            if (error instanceof AccessControlException) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else if (error instanceof SecurityException) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
            sendResponse(resp.getWriter(), error);
        }
    }

    protected abstract boolean handle(
            String command, HttpServletRequest req, HttpServletResponse resp) throws Exception;

    public long getUserId(HttpServletRequest req) throws Exception  {
        Object userId = req.getSession().getAttribute(USER_ID_KEY);
        if (userId != null) {
            return (Long) userId;
        }
        throw new AccessControlException("User not logged in");
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

    public void sendResponse(HttpServletResponse resp, Collection collection) throws IOException {
        if (collection.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("success", true);
        result.add("data", JsonConverter.arrayToJson(collection));
        resp.getWriter().write(result.build().toString());
    }

    public void sendResponse(Writer writer, Exception error) throws IOException {
        JsonObjectBuilder result = Json.createObjectBuilder();
        result.add("success", false);
        result.add("error", Log.exceptionStack(error));
        writer.write(result.build().toString());
    }

    protected String getCommand(HttpServletRequest req) {
        String command = req.getPathInfo();
        if (command == null) {
            command = "";
        }
        return command;
    }

}
