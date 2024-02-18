/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.traccar.api.resource.SessionResource;
import org.traccar.api.security.LoginResult;
import org.traccar.api.security.LoginService;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.User;
import org.traccar.session.ConnectionManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.web.HttpSessionCollector;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.*;

@Singleton
public class AsyncSocketServlet extends JettyWebSocketServlet {

    // private static final Logger LOGGER =
    // LoggerFactory.getLogger(AsyncSocketServlet.class);
    private final Config config;
    private final ObjectMapper objectMapper;
    private final ConnectionManager connectionManager;
    private final Storage storage;
    private static final String KEY_TOKEN = "token";
    private static final String KEY_SESSION = "session";

    @Inject
    private LoginService loginService;

    @Inject
    public AsyncSocketServlet(
            Config config, ObjectMapper objectMapper, ConnectionManager connectionManager, Storage storage) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.connectionManager = connectionManager;
        this.storage = storage;
    }

    @Override
    public void configure(JettyWebSocketServletFactory factory) {
        factory.setIdleTimeout(Duration.ofMillis(config.getLong(Keys.WEB_TIMEOUT)));
        factory.setCreator((req, resp) -> {
            if (req.getSession() != null) {
                Long userId = (Long) ((HttpSession) req.getSession()).getAttribute(SessionResource.USER_ID_KEY);
                if (userId != null) {
                    return new AsyncSocket(objectMapper, connectionManager, storage, userId);
                }
            } else if (req.getQueryString().contains(KEY_SESSION)) {
                Map<String, String> params = getQueryMap(req.getQueryString());
                String sessionID = (String) params.get(KEY_SESSION);
                HttpSession httpSession = HttpSessionCollector.find(sessionID);
                Long userId = (Long) httpSession.getAttribute(SessionResource.USER_ID_KEY);
                if (userId != null) {
                    return new AsyncSocket(objectMapper, connectionManager, storage, userId);
                }
            } else if (req.getQueryString().contains(KEY_TOKEN)) { // not secure
                Map<String, String> params = getQueryMap(req.getQueryString());
                String token = (String) params.get(KEY_TOKEN);
                LoginResult loginResult;
                try {
                    loginResult = loginService.login(token);
                } catch (StorageException | GeneralSecurityException | IOException e) {
                    return null;
                }
                if (loginResult.getUser() != null) {
                    return new AsyncSocket(objectMapper, connectionManager, storage, loginResult.getUser().getId());
                }
            }
            return null;
        });
    }

    public static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            String[] p = param.split("=");
            String name = p[0];
            if (p.length > 1) {
                String value = p[1];
                map.put(name, value);
            }
        }
        return map;
    }

}
