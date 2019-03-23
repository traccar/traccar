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
import java.sql.SQLException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.traccar.Context;
import org.traccar.Main;
import org.traccar.api.resource.SessionResource;
import org.traccar.database.StatisticsManager;
import org.traccar.helper.Log;
import org.traccar.model.Device;

public class MediaFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        try {
            HttpSession session = ((HttpServletRequest) request).getSession(false);
            Long userId = null;
            if (session != null) {
                userId = (Long) session.getAttribute(SessionResource.USER_ID_KEY);
                if (userId != null) {
                    Context.getPermissionsManager().checkUserEnabled(userId);
                    Main.getInjector().getInstance(StatisticsManager.class).registerRequest(userId);
                }
            }
            if (userId == null) {
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            String path = ((HttpServletRequest) request).getPathInfo();
            String[] parts = path.split("/");
            if (parts.length < 2 || parts.length == 2 && !path.endsWith("/")) {
                Context.getPermissionsManager().checkAdmin(userId);
            } else {
                Device device = Context.getDeviceManager().getByUniqueId(parts[1]);
                if (device != null) {
                    Context.getPermissionsManager().checkDevice(userId, device.getId());
                } else {
                    httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }

            chain.doFilter(request, response);
        } catch (SecurityException e) {
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.getWriter().println(Log.exceptionStack(e));
        } catch (SQLException e) {
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.getWriter().println(Log.exceptionStack(e));
        }
    }

    @Override
    public void destroy() {
    }

}
