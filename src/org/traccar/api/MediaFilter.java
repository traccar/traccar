/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
 * Copyright 2018 Andrey Kunitsyn (andrey@traccar.org)
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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.NotAuthorizedException;

import org.traccar.Context;
import org.traccar.api.resource.SessionResource;
import org.traccar.helper.Log;
import org.traccar.model.Device;

public class MediaFilter implements Filter {

    private boolean dirAllowed;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        dirAllowed = Context.getConfig().getBoolean("media.dirAllowed");
    }

    private static void formatError(HttpServletResponse response, Exception e) throws IOException {
        if (e instanceof SecurityException) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else if (e instanceof IllegalArgumentException) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else if (e instanceof NotAuthorizedException) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        response.getWriter().println(Log.exceptionStack(e));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpSession session = ((HttpServletRequest) request).getSession(false);
            Long userId = null;
            if (session != null) {
                userId = (Long) session.getAttribute(SessionResource.USER_ID_KEY);
                if (userId != null) {
                    Context.getPermissionsManager().checkUserEnabled(userId);
                    Context.getStatisticsManager().registerRequest(userId);
                }
            }
            if (userId == null) {
                throw new NotAuthorizedException("Not authorized");
            }

            String[] parts = ((HttpServletRequest) request).getPathInfo().split("/");
            if (parts.length < 2) {
                if (dirAllowed) {
                    Context.getPermissionsManager().checkAdmin(userId);
                } else {
                    throw new SecurityException("Wrong path");
                }
            } else if (parts.length == 2 && !dirAllowed) {
                throw new SecurityException("Wrong path");
            } else {
                Device device = Context.getIdentityManager().getByUniqueId(parts[1]);
                if (device != null) {
                    Context.getPermissionsManager().checkDevice(userId, device.getId());
                } else {
                    throw new IllegalArgumentException("Device not found");
                }
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            formatError((HttpServletResponse) response, e);
        }
    }

    @Override
    public void destroy() {
    }

}
