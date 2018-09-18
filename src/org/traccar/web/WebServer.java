/*
 * Copyright 2012 - 2018 Anton Tananaev (anton@traccar.org)
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

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.proxy.AsyncProxyServlet;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Config;
import org.traccar.Context;
import org.traccar.api.AsyncSocketServlet;
import org.traccar.api.CorsResponseFilter;
import org.traccar.api.MediaFilter;
import org.traccar.api.ObjectMapperProvider;
import org.traccar.api.ResourceErrorHandler;
import org.traccar.api.SecurityRequestFilter;
import org.traccar.api.resource.ServerResource;

import javax.naming.InitialContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.EnumSet;

public class WebServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

    private Server server;
    private final Config config;
    private final DataSource dataSource;
    private final HandlerList handlers = new HandlerList();
    private int sessionTimeout;

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
        sessionTimeout = config.getInteger("web.sessionTimeout");

        initServer();
        initApi();
        if (config.getBoolean("web.console")) {
            initConsole();
        }
        switch (config.getString("web.type", "new")) {
            case "old":
                initOldWebApp();
                break;
            default:
                initWebApp();
                break;
        }
        initClientProxy();
        server.setHandler(handlers);

        server.addBean(new ErrorHandler() {
            @Override
            protected void handleErrorPage(
                    HttpServletRequest request, Writer writer, int code, String message) throws IOException {
                writer.write("<!DOCTYPE<html><head><title>Error</title></head><html><body>"
                        + code + " - " + HttpStatus.getMessage(code) + "</body></html>");
            }
        }, false);
    }

    private void initClientProxy() {
        int port = Context.getConfig().getInteger("osmand.port");
        if (port != 0) {
            ServletContextHandler servletHandler = new ServletContextHandler() {
                @Override
                public void doScope(
                        String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                        throws IOException, ServletException {
                    if (target.equals("/") && request.getMethod().equals(HttpMethod.POST.asString())) {
                        super.doScope(target, baseRequest, request, response);
                    }
                }
            };
            ServletHolder servletHolder = new ServletHolder(new AsyncProxyServlet.Transparent());
            servletHolder.setInitParameter("proxyTo", "http://localhost:" + port);
            servletHandler.addServlet(servletHolder, "/");
            handlers.addHandler(servletHandler);
        }
    }

    private void initWebApp() {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase(config.getString("web.path"));
        if (config.getBoolean("web.debug")) {
            resourceHandler.setWelcomeFiles(new String[] {"debug.html", "index.html"});
        } else {
            String cache = config.getString("web.cacheControl");
            if (cache != null && !cache.isEmpty()) {
                resourceHandler.setCacheControl(cache);
            }
            resourceHandler.setWelcomeFiles(new String[] {"release.html", "index.html"});
        }
        handlers.addHandler(resourceHandler);
    }

    private void initOldWebApp() {
        try {
            javax.naming.Context context = new InitialContext();
            context.bind("java:/DefaultDS", dataSource);
        } catch (Exception error) {
            LOGGER.warn("JNDI context error", error);
        }

        WebAppContext app = new WebAppContext();
        app.setContextPath("/");
        if (sessionTimeout > 0) {
            app.getSessionHandler().setMaxInactiveInterval(sessionTimeout);
        }
        app.setWar(config.getString("web.application"));
        handlers.addHandler(app);
    }

    private void initApi() {
        ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletHandler.setContextPath("/api");
        if (sessionTimeout > 0) {
            servletHandler.getSessionHandler().setMaxInactiveInterval(sessionTimeout);
        }

        servletHandler.addServlet(new ServletHolder(new AsyncSocketServlet()), "/socket");

        if (config.hasKey("media.path")) {
            ServletHolder servletHolder = new ServletHolder("media", DefaultServlet.class);
            servletHolder.setInitParameter("resourceBase", config.getString("media.path"));
            servletHolder.setInitParameter("dirAllowed", config.getString("media.dirAllowed", "false"));
            servletHolder.setInitParameter("pathInfoOnly", "true");
            servletHandler.addServlet(servletHolder, "/media/*");
            servletHandler.addFilter(MediaFilter.class, "/media/*", EnumSet.allOf(DispatcherType.class));
        }

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.registerClasses(JacksonFeature.class, ObjectMapperProvider.class, ResourceErrorHandler.class);
        resourceConfig.registerClasses(SecurityRequestFilter.class, CorsResponseFilter.class);
        resourceConfig.packages(ServerResource.class.getPackage().getName());
        servletHandler.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/*");

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
            LOGGER.warn("Web server start failed", error);
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception error) {
            LOGGER.warn("Web server stop failed", error);
        }
    }

}
