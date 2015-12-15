/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.traccar.Config;
import org.traccar.api.AsyncSocketServlet;
import org.traccar.api.CorsResponseFilter;
import org.traccar.api.ResourceErrorHandler;
import org.traccar.api.SecurityRequestFilter;
import org.traccar.api.resource.CommandResource;
import org.traccar.api.resource.DeviceResource;
import org.traccar.api.resource.PermissionResource;
import org.traccar.api.resource.PositionResource;
import org.traccar.api.resource.ServerResource;
import org.traccar.api.resource.SessionResource;
import org.traccar.api.resource.UserResource;
import org.traccar.helper.Log;

public class WebServer {

    private Server server;
    private final Config config;
    private final DataSource dataSource;
    private final HandlerList handlers = new HandlerList();

    private void initServer() {

        String address = config.getString("web.address");
        int port = config.getInteger("web.port", 8082);
        if (address == null) {
            server = new Server(port);
        } else {
            server = new Server(new InetSocketAddress(address, port));
        }
    }

    public WebServer(Config config, DataSource dataSource) {
        this.config = config;
        this.dataSource = dataSource;

        initServer();
        switch (config.getString("web.type", "new")) {
            case "api":
                initApi();
                break;
            case "old":
                initOldApi();
                initOldWebApp();
                break;
            default:
                initApi();
                if (config.getBoolean("web.console")) {
                    initConsole();
                }
                initWebApp();
                break;
        }
        server.setHandler(handlers);

        server.addBean(new ErrorHandler() {
            @Override
            protected void handleErrorPage(
                    HttpServletRequest request, Writer writer, int code, String message) throws IOException {
                writer.write("<!DOCTYPE<html><head><title>Error</title></head><html><body>"
                        + code + " - " + HttpStatus.getMessage(code) + "</body></html>");
            }
        });
    }

    private void initWebApp() {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase(config.getString("web.path"));
        if (config.getBoolean("web.debug")) {
            resourceHandler.setWelcomeFiles(new String[] {"debug.html"});
        } else {
            resourceHandler.setWelcomeFiles(new String[] {"release.html", "index.html"});
        }
        handlers.addHandler(resourceHandler);
    }

    private void initOldWebApp() {
        try {
            javax.naming.Context context = new InitialContext();
            context.bind("java:/DefaultDS", dataSource);
        } catch (Exception error) {
            Log.warning(error);
        }

        WebAppContext app = new WebAppContext();
        app.setContextPath("/");
        app.setWar(config.getString("web.application"));
        handlers.addHandler(app);
    }

    private void initApi() {
        ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletHandler.setContextPath("/api");

        servletHandler.addServlet(new ServletHolder(new AsyncSocketServlet()), "/socket");

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(ResourceErrorHandler.class);
        resourceConfig.register(SecurityRequestFilter.class);
        resourceConfig.register(CorsResponseFilter.class);
        resourceConfig.registerClasses(ServerResource.class, SessionResource.class, CommandResource.class,
                PermissionResource.class, DeviceResource.class, UserResource.class, PositionResource.class);
        servletHandler.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/*");

        handlers.addHandler(servletHandler);
    }

    private void initOldApi() {
        ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletHandler.setContextPath("/api");
        servletHandler.addServlet(new ServletHolder(new AsyncServlet()), "/async/*");
        servletHandler.addServlet(new ServletHolder(new ServerServlet()), "/server/*");
        servletHandler.addServlet(new ServletHolder(new UserServlet()), "/user/*");
        servletHandler.addServlet(new ServletHolder(new DeviceServlet()), "/device/*");
        servletHandler.addServlet(new ServletHolder(new PositionServlet()), "/position/*");
        servletHandler.addServlet(new ServletHolder(new CommandServlet()), "/command/*");
        servletHandler.addServlet(new ServletHolder(new MainServlet()), "/*");
        handlers.addHandler(servletHandler);
    }

    private void initConsole() {
        ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletHandler.setContextPath("/console");
        servletHandler.addServlet(new ServletHolder(new ConsoleServlet()), "/*");
        handlers.addHandler(servletHandler);
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
