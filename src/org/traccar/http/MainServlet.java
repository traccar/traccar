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
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.traccar.database.DataManager;
import org.traccar.helper.Log;

public class MainServlet extends HttpServlet {

    private static final String USER_ID = "userId";

    private final DataManager dataManager;

    public MainServlet(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String command = req.getPathInfo();

        if (command.equals("/async")) {
            async(req.startAsync());
        } else if (command.equals("/login")) {
            login(req, resp);
        } else if (command.equals("/logout")) {
            logout(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
    
    private void async(final AsyncContext context) {
        context.start(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000); // DELME

                    ServletResponse response = context.getResponse();
                    response.getWriter().println("{ success: true, data: ["+
                            "{ id: 1, device_id: 1, time: \"2012-04-23T18:25:43.511Z\", latitude: 60, longitude: 30, speed: 0, course: 0 }," +
                            "{ id: 2, device_id: 2, time: \"2012-04-23T19:25:43.511Z\", latitude: 61, longitude: 31, speed: 0, course: 0 }" +
                            "] }");
                    context.complete();

                } catch (Exception ex) {
                    Log.warning(ex);
                }
            }
        });
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.getSession().setAttribute(USER_ID,
                    dataManager.login(req.getParameter("name"), req.getParameter("password")));
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
