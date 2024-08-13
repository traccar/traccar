/*
 * Copyright 2012 - 2023 Anton Tananaev (anton@traccar.org)
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

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.proxy.AsyncProxyServlet;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.DatabaseAdaptor;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.JDBCSessionDataStoreFactory;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.LifecycleObject;
import org.traccar.api.CorsResponseFilter;
import org.traccar.api.DateParameterConverterProvider;
import org.traccar.api.ResourceErrorHandler;
import org.traccar.api.resource.ServerResource;
import org.traccar.api.security.SecurityRequestFilter;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.ObjectMapperContextResolver;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.EnumSet;

public class WebServer implements LifecycleObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

    private final Injector injector;
    private final Config config;
    private final Server server;

    public WebServer(Injector injector, Config config) {
        this.injector = injector;
        this.config = config;
        String address = config.getString(Keys.WEB_ADDRESS);
        int port = config.getInteger(Keys.WEB_PORT);
        if (address == null) {
            server = new Server(port);
        } else {
            server = new Server(new InetSocketAddress(address, port));
        }

        ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        JettyWebSocketServletContainerInitializer.configure(servletHandler, null);
        servletHandler.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));

        initApi(servletHandler);
        initSessionConfig(servletHandler);

        if (config.getBoolean(Keys.WEB_CONSOLE)) {
            servletHandler.addServlet(new ServletHolder(new ConsoleServlet(config)), "/console/*");
        }

        initWebApp(servletHandler);

        servletHandler.setErrorHandler(new ErrorHandler() {
            @Override
            protected void handleErrorPage(
                    HttpServletRequest request, Writer writer, int code, String message) throws IOException {
                writer.write("<!DOCTYPE><html><head><title>Error</title></head><html><body>"
                        + code + " - " + HttpStatus.getMessage(code) + "</body></html>");
            }
        });

        HandlerList handlers = new HandlerList();
        initClientProxy(handlers);
        handlers.addHandler(servletHandler);
        handlers.addHandler(new GzipHandler());
        server.setHandler(handlers);

        if (config.hasKey(Keys.WEB_REQUEST_LOG_PATH)) {
            RequestLogWriter logWriter = new RequestLogWriter(config.getString(Keys.WEB_REQUEST_LOG_PATH));
            logWriter.setAppend(true);
            logWriter.setRetainDays(config.getInteger(Keys.WEB_REQUEST_LOG_RETAIN_DAYS));
            server.setRequestLog(new WebRequestLog(logWriter));
        }
    }

    private void initClientProxy(HandlerList handlers) {
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
            ServletHolder servletHolder = new ServletHolder(AsyncProxyServlet.Transparent.class);
            servletHolder.setInitParameter("proxyTo", "http://localhost:" + port);
            servletHandler.addServlet(servletHolder, "/");
            handlers.addHandler(servletHandler);
        }
    }

    private void initWebApp(ServletContextHandler servletHandler) {
        ServletHolder servletHolder = new ServletHolder(new DefaultOverrideServlet(config));
        servletHolder.setInitParameter("resourceBase", new File(config.getString(Keys.WEB_PATH)).getAbsolutePath());
        servletHolder.setInitParameter("dirAllowed", "false");
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

    private void initApi(ServletContextHandler servletHandler) {
        String mediaPath = config.getString(Keys.MEDIA_PATH);
        if (mediaPath != null) {
            ServletHolder servletHolder = new ServletHolder(DefaultServlet.class);
            servletHolder.setInitParameter("resourceBase", new File(mediaPath).getAbsolutePath());
            servletHolder.setInitParameter("dirAllowed", "false");
            servletHolder.setInitParameter("pathInfoOnly", "true");
            servletHandler.addServlet(servletHolder, "/api/media/*");
        }

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.registerClasses(
                JacksonFeature.class,
                ObjectMapperContextResolver.class,
                DateParameterConverterProvider.class,
                SecurityRequestFilter.class,
                CorsResponseFilter.class,
                ResourceErrorHandler.class);
        resourceConfig.packages(ServerResource.class.getPackage().getName());
        if (resourceConfig.getClasses().stream().filter(ServerResource.class::equals).findAny().isEmpty()) {
            LOGGER.warn("Failed to load API resources");
        }
        servletHandler.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/api/*");
    }

    private void initSessionConfig(ServletContextHandler servletHandler) {
        if (config.getBoolean(Keys.WEB_PERSIST_SESSION)) {
            DatabaseAdaptor databaseAdaptor = new DatabaseAdaptor();
            databaseAdaptor.setDatasource(injector.getInstance(DataSource.class));
            JDBCSessionDataStoreFactory jdbcSessionDataStoreFactory = new JDBCSessionDataStoreFactory();
            jdbcSessionDataStoreFactory.setDatabaseAdaptor(databaseAdaptor);
            SessionHandler sessionHandler = servletHandler.getSessionHandler();
            SessionCache sessionCache = new DefaultSessionCache(sessionHandler);
            sessionCache.setSessionDataStore(jdbcSessionDataStoreFactory.getSessionDataStore(sessionHandler));
            sessionHandler.setSessionCache(sessionCache);
        }

        SessionCookieConfig sessionCookieConfig = servletHandler.getServletContext().getSessionCookieConfig();

        int sessionTimeout = config.getInteger(Keys.WEB_SESSION_TIMEOUT);
        if (sessionTimeout > 0) {
            servletHandler.getSessionHandler().setMaxInactiveInterval(sessionTimeout);
            sessionCookieConfig.setMaxAge(sessionTimeout);
        }

        String sameSiteCookie = config.getString(Keys.WEB_SAME_SITE_COOKIE);
        if (sameSiteCookie != null) {
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

    @Override
    public void start() throws Exception {
        server.start();
    }

    @Override
    public void stop() throws Exception {
        server.stop();
    }

}
