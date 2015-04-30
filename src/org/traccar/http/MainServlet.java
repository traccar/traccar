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
package org.traccar.http;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.traccar.Context;
import org.traccar.GlobalTimer;
import org.traccar.database.DataCache;
import org.traccar.database.ObjectConverter;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public class MainServlet extends HttpServlet {

    public static final String USER_ID = "userId";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String command = req.getPathInfo();

        if (command == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } else if (command.equals("/session")) {
            session(req, resp);
        } else if (command.equals("/login")) {
            login(req, resp);
        } else if (command.equals("/logout")) {
            logout(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void session(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().println("{ success: true, session: " + (req.getSession().getAttribute(USER_ID) != null) + " }");
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.getSession().setAttribute(USER_ID,
                    Context.getDataManager().login(req.getParameter("email"), req.getParameter("password")));
            resp.getWriter().println("{ success: true }");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.getSession().removeAttribute(USER_ID);
        resp.getWriter().println("{ success: true }");
    }

}
