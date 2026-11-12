/*
 * Copyright 2015 - 2025 Anton Tananaev (anton@traccar.org)
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
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory;
import org.traccar.api.security.LoginResult;
import org.traccar.api.security.LoginService;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.session.ConnectionManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.List;

@Singleton
public class AsyncSocketServlet extends JettyWebSocketServlet {

    private final Config config;
    private final ObjectMapper objectMapper;
    private final ConnectionManager connectionManager;
    private final Storage storage;
    private final LoginService loginService;

    @Inject
    public AsyncSocketServlet(
            Config config, ObjectMapper objectMapper, ConnectionManager connectionManager, Storage storage,
            LoginService loginService) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.connectionManager = connectionManager;
        this.storage = storage;
        this.loginService = loginService;
    }

    @Override
    public void configure(JettyWebSocketServletFactory factory) {
        factory.setIdleTimeout(Duration.ofMillis(config.getLong(Keys.WEB_TIMEOUT)));
        factory.setCreator((req, resp) -> {
            try {
                List<String> tokens = req.getParameterMap().get("token");
                LoginResult loginResult = tokens != null && !tokens.isEmpty()
                        ? loginService.login(tokens.iterator().next())
                        : loginService.login(req.getHttpServletRequest());
                if (loginResult != null && loginResult.getUser() != null) {
                    return new AsyncSocket(objectMapper, connectionManager, storage, loginResult.getUser().getId());
                }
            } catch (SecurityException e) {
                return null;
            } catch (StorageException | GeneralSecurityException | IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

}
