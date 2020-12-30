/*
 * Copyright 2012 - 2020 Anton Tananaev (anton@traccar.org)
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

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.proxy.AsyncProxyServlet;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.api.DateParameterConverterProvider;
import org.traccar.config.Config;
import org.traccar.api.AsyncSocketServlet;
import org.traccar.api.CorsResponseFilter;
import org.traccar.api.MediaFilter;
import org.traccar.api.ObjectMapperProvider;
import org.traccar.api.ResourceErrorHandler;
import org.traccar.api.SecurityRequestFilter;
import org.traccar.api.resource.ServerResource;
import org.traccar.config.Keys;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.EnumSet;

public class WebServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

    private Server server;

    private void initServer(Config config) {

        String address = config.getString(Keys.WEB_ADDRESS);
        int port = config.getInteger(Keys.WEB_PORT);
        if (address == null) {
            server = new Server(port);
        } else {
            server = new Server(new InetSocketAddress(address, port));
        }
    }

    public WebServer(Config config) {

        initServer(config);

        ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);

        initApi(config, servletHandler);
        initSessionConfig(config, servletHandler);

        if (config.getBoolean(Keys.WEB_CONSOLE)) {
            servletHandler.addServlet(new ServletHolder(new ConsoleServlet()), "/console/*");
        }

        initWebApp(config, servletHandler);

        servletHandler.setErrorHandler(new ErrorHandler() {
            @Override
            protected void handleErrorPage(
                    HttpServletRequest request, Writer writer, int code, String message) throws IOException {
                writer.write("<!DOCTYPE><html><head><title>Error</title></head><html><body>"
                        + code + " - " + HttpStatus.getMessage(code) + "</body></html>");
            }
        });

        HandlerList handlers = new HandlerList();
        initClientProxy(config, handlers);
        handlers.addHandler(servletHandler);
        handlers.addHandler(new GzipHandler());
        server.setHandler(handlers);

        if (config.getBoolean(Keys.WEB_REQUEST_LOG_ENABLE)) {
            RequestLogWriter logWriter = new RequestLogWriter(config.getString(Keys.WEB_REQUEST_LOG_PATH));
            logWriter.setAppend(true);
            logWriter.setRetainDays(config.getInteger(Keys.WEB_REQUEST_LOG_RETAIN_DAYS));
            CustomRequestLog requestLog = new CustomRequestLog(logWriter, CustomRequestLog.NCSA_FORMAT);
            server.setRequestLog(requestLog);
        }
    }

    private void initClientProxy(Config config, HandlerList handlers) {
        int port = config.getInteger(Keys.PROTOCOL_PORT.withPrefix("osmand"));
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

    private void initWebApp(Config config, ServletContextHandler servletHandler) {
        ServletHolder servletHolder = new ServletHolder(DefaultServlet.class);
        servletHolder.setInitParameter("resourceBase", new File(config.getString(Keys.WEB_PATH)).getAbsolutePath());
        if (config.getBoolean(Keys.WEB_DEBUG)) {
            servletHandler.setWelcomeFiles(new String[] {"debug.html", "index.html"});
        } else {
            String cache = config.getString(Keys.WEB_CACHE_CONTROL);
            if (cache != null && !cache.isEmpty()) {
                servletHolder.setInitParameter("cacheControl", cache);
            }
            servletHandler.setWelcomeFiles(new String[] {"release.html", "index.html"});
        }
        servletHandler.addServlet(servletHolder, "/*");
    }

    private void initApi(Config config, ServletContextHandler servletHandler) {
        servletHandler.addServlet(new ServletHolder(new AsyncSocketServlet()), "/api/socket");

        String mediaPath = config.getString(Keys.MEDIA_PATH);
        if (mediaPath != null) {
            ServletHolder servletHolder = new ServletHolder(DefaultServlet.class);
            servletHolder.setInitParameter("resourceBase", new File(mediaPath).getAbsolutePath());
            servletHolder.setInitParameter("dirAllowed", "false");
            servletHolder.setInitParameter("pathInfoOnly", "true");
            servletHandler.addServlet(servletHolder, "/api/media/*");
            servletHandler.addFilter(MediaFilter.class, "/api/media/*", EnumSet.allOf(DispatcherType.class));
        }

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.registerClasses(
                JacksonFeature.class, ObjectMapperProvider.class, ResourceErrorHandler.class,
                SecurityRequestFilter.class, CorsResponseFilter.class, DateParameterConverterProvider.class);
        resourceConfig.packages(ServerResource.class.getPackage().getName());
        servletHandler.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/api/*");
    }

    private void initSessionConfig(Config config, ServletContextHandler servletHandler) {
        int sessionTimeout = config.getInteger(Keys.WEB_SESSION_TIMEOUT);
        if (sessionTimeout > 0) {
            servletHandler.getSessionHandler().setMaxInactiveInterval(sessionTimeout);
        }

        String sameSiteCookie = config.getString(Keys.WEB_SAME_SITE_COOKIE);
        if (sameSiteCookie != null) {
            SessionCookieConfig sessionCookieConfig = servletHandler.getServletContext().getSessionCookieConfig();
            switch (sameSiteCookie.toLowerCase()) {
                case "lax":
                    sessionCookieConfig.setComment(HttpCookie.SAME_SITE_LAX_COMMENT);
                    break;
                case "strict":
                    sessionCookieConfig.setComment(HttpCookie.SAME_SITE_STRICT_COMMENT);
                    break;
                case "none":
                    sessionCookieConfig.setSecure(true);
                    sessionCookieConfig.setComment(HttpCookie.SAME_SITE_NONE_COMMENT);
                    break;
                default:
                    break;
            }
        }
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
