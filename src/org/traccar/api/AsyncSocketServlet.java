/*
 * Copyright 2015 - 2016 Anton Tananaev (anton@traccar.org)
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

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.traccar.Context;
import org.traccar.api.resource.SessionResource;
import org.traccar.helper.Log;
import org.traccar.model.User;

import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.lang.reflect.Method;
import java.sql.SQLException;


public class AsyncSocketServlet extends WebSocketServlet {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    public static final String BASIC_REALM = "Basic realm=\"api\"";
    public static final String X_REQUESTED_WITH = "X-Requested-With";
    public static final String XML_HTTP_REQUEST = "XMLHttpRequest";
    private static final long ASYNC_TIMEOUT = 10 * 60 * 1000;

    @javax.ws.rs.core.Context
    private HttpServletRequest request;

    @javax.ws.rs.core.Context
    private ResourceInfo resourceInfo;

    @javax.ws.rs.core.Context
    private SecurityContext securityContext;

    protected long getUserId() {
        UserPrincipal principal = (UserPrincipal) securityContext.getUserPrincipal();
        if (principal != null) {
            return principal.getUserId();
        }
        return 0;
    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(Context.getConfig().getLong("web.timeout", ASYNC_TIMEOUT));
        factory.setCreator(new WebSocketCreator() {
            @Override
            public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {

                auth(req);
                return new AsyncSocket(getUserId());

            }
        });
    }

    private void auth(ServletUpgradeRequest req) {

        request = req.getHttpServletRequest();

        if (req.getHttpServletRequest().getMethod().equals("OPTIONS")) {
            return;
        }

        SecurityContext securityContext = null;

        try {

            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (authHeader != null) {

                try {
                    String[] auth = SecurityRequestFilter.decodeBasicAuth(authHeader);
                    User user = Context.getPermissionsManager().login(auth[0], auth[1]);
                    if (user != null) {
                        Context.getStatisticsManager().registerRequest(user.getId());
                        securityContext = new UserSecurityContext(new UserPrincipal(user.getId()));
                    }
                } catch (SQLException e) {
                    throw new WebApplicationException(e);
                }

            } else if (req.getSession() != null) {

                Long userId = (Long) req.getSession().getAttribute(SessionResource.USER_ID_KEY);
                if (userId != null) {
                    Context.getPermissionsManager().checkUserEnabled(userId);
                    Context.getStatisticsManager().registerRequest(userId);
                    securityContext = new UserSecurityContext(new UserPrincipal(userId));
                }

            }

        } catch (SecurityException e) {
            Log.warning(e);
        }

        if (securityContext != null) {
            this.securityContext = securityContext;
        } else {
            Method method = resourceInfo.getResourceMethod();
            if (!method.isAnnotationPresent(PermitAll.class)) {
                Response.ResponseBuilder responseBuilder = Response.status(Response.Status.UNAUTHORIZED);
                if (!XML_HTTP_REQUEST.equals(request.getHeader(X_REQUESTED_WITH))) {
                    responseBuilder.header(WWW_AUTHENTICATE, BASIC_REALM);
                }
                throw new WebApplicationException(responseBuilder.build());
            }
        }

    }

}
