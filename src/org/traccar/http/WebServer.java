/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.traccar.helper.Log;
import org.traccar.model.DataManager;
import org.traccar.model.Device;
import org.traccar.model.Position;

/**
 * Integrated HTTP server
 */
public class WebServer {

    private Server server;

    private void initDataSource(Properties properties) {
        try {

            Context context = new InitialContext();

            Class clazz = Class.forName(properties.getProperty("database.dataSource"));

            DataSource ds = (DataSource) clazz.newInstance();
            clazz.getMethod("setURL", String.class).invoke(ds, properties.getProperty("database.url"));
            clazz.getMethod("setUser", String.class).invoke(ds, properties.getProperty("database.user"));
            clazz.getMethod("setPassword", String.class).invoke(ds, properties.getProperty("database.password"));

            context.bind("java:/DefaultDS", ds);

        } catch (Exception error) {
            Log.warning(error.getMessage());
        }
    }

    public WebServer(Properties properties) {
        String address = properties.getProperty("http.address");
        Integer port = Integer.valueOf(properties.getProperty("http.port", "8082"));
        if (address == null) {
            server = new Server(port);
        } else {
            server = new Server(new InetSocketAddress(address, port));
        }

        initDataSource(properties);

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(properties.getProperty("http.application"));
        server.setHandler(webapp);
    }

    public void start() {
        try {
            server.start();
        } catch (Exception error) {
            Log.warning(error.getMessage());
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception error) {
            Log.warning(error.getMessage());
        }
    }

}
