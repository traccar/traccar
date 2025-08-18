/*
 * Copyright 2022 - 2023 Anton Tananaev (anton@traccar.org)
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

import org.eclipse.jetty.ee10.servlets.DoSFilter;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Singleton
public class ThrottlingFilter extends DoSFilter {

    @Inject
    private Config config;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        if (config.hasKey(Keys.WEB_MAX_REQUESTS_PER_SECOND)) {
            setMaxRequestsPerSec(config.getInteger(Keys.WEB_MAX_REQUESTS_PER_SECOND));
        }
        setMaxRequestMs(config.getInteger(Keys.WEB_MAX_REQUEST_SECONDS) * 1000L);
    }

    @Override
    protected String extractUserId(ServletRequest request) {
        HttpSession session = ((HttpServletRequest) request).getSession(false);
        if (session != null) {
            var userId = session.getAttribute("userId");
            return userId != null ? userId.toString() : null;
        }
        return null;
    }
}
