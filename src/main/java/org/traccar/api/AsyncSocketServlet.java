/*
 * Copyright 2015 - 2021 Anton Tananaev (anton@traccar.org)
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

import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;
import org.traccar.Context;
import org.traccar.api.resource.SessionResource;
import org.traccar.config.Keys;
import org.traccar.model.User;
import org.traccar.storage.StorageException;

import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import java.time.Duration;

import static org.traccar.api.security.SecurityRequestFilter.AUTHORIZATION_HEADER;
import static org.traccar.api.security.SecurityRequestFilter.decodeBasicAuth;

public class AsyncSocketServlet extends JettyWebSocketServlet {

    @Override
    public void configure(JettyWebSocketServletFactory factory) {
        factory.setIdleTimeout(Duration.ofMillis(Context.getConfig().getLong(Keys.WEB_TIMEOUT)));
        factory.setCreator((req, resp) -> {
            String authHeader = req.getHeader(AUTHORIZATION_HEADER);
            if (authHeader != null) {
                try {
                    String[] auth = decodeBasicAuth(authHeader);
                    User user = Context.getPermissionsManager().login(auth[0], auth[1]);

                    if (user != null) {
                        return new AsyncSocket(user.getId());
                    }
                    return  null;
                } catch (StorageException e) {
                    throw new WebApplicationException(e);
                }
            } else {
                if (req.getSession() != null) {
                    long userId = (Long) ((HttpSession) req.getSession()).getAttribute(SessionResource.USER_ID_KEY);
                    return new AsyncSocket(userId);
                } else {
                    return null;
                }
            }
        });
    }
}