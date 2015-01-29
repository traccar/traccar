/*
 * Copyright 2012 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.traccar.helper.Log;

/**
 * Integrated HTTP server
 */
public class WebServer {

    private Server server;

    public WebServer(Properties properties, DataSource dataSource) {
        String address = properties.getProperty("http.address");
        Integer port = Integer.valueOf(properties.getProperty("http.port", "8082"));
        if (address == null) {
            server = new Server(port);
        } else {
            server = new Server(new InetSocketAddress(address, port));
        }

        if (Boolean.valueOf(properties.getProperty("http.new"))) {

            ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            servletHandler.setContextPath("/api");
            servletHandler.addServlet(new ServletHolder(new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                    PrintWriter out = resp.getWriter();
                    out.println("<html><body>api</body></html>");
                }
            }), "/*");

            ResourceHandler resourceHandler = new ResourceHandler();
            resourceHandler.setResourceBase(properties.getProperty("http.path"));
            resourceHandler.setWelcomeFiles(new String[] {"index.html"});

            HandlerList handlerList = new HandlerList();
            handlerList.setHandlers(new Handler[] {servletHandler, resourceHandler});

            server.setHandler(handlerList);

        } else {

            try {
                Context context = new InitialContext();
                context.bind("java:/DefaultDS", dataSource);
            } catch (Exception error) {
                Log.warning(error);
            }

            WebAppContext webapp = new WebAppContext();
            webapp.setContextPath("/");
            webapp.setWar(properties.getProperty("http.application"));
            server.setHandler(webapp);

        }
    }

    public void start() {
        try {
            server.start();
        } catch (Exception error) {
            Log.warning(error);
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception error) {
            Log.warning(error);
        }
    }

}
