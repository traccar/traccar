/*
 * Copyright 2015 - 2020 Anton Tananaev (anton@traccar.org)
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

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.traccar.Context;
import org.traccar.api.resource.SessionResource;
import org.traccar.config.Keys;

public class AsyncSocketServlet extends WebSocketServlet {

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(Context.getConfig().getLong(Keys.WEB_TIMEOUT));
        factory.setCreator((req, resp) -> {
            if (req.getSession() != null) {
                long userId = (Long) req.getSession().getAttribute(SessionResource.USER_ID_KEY);
                return new AsyncSocket(userId);
            } else {
                return null;
            }
        });
    }

}
