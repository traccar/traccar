/*
 * Copyright 2025 Anton Tananaev (anton@traccar.org)
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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.traccar.api.security.LoginService;

import java.io.IOException;
import java.util.Locale;

public class McpAuthFilter implements Filter {

    public static final String ATTRIBUTE_USER = "mcpUser";

    private final LoginService loginService;

    public McpAuthFilter(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String authorization = httpRequest.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            unauthorized(httpResponse, "Missing Authorization header");
            return;
        }

        String[] parts = authorization.split(" ", 2);
        if (parts.length != 2 || !"bearer".equals(parts[0].toLowerCase(Locale.ROOT)) || parts[1].isBlank()) {
            unauthorized(httpResponse, "Authorization header must be a Bearer token");
            return;
        }

        try {
            var loginResult = loginService.login(parts[1].trim());
            if (loginResult == null || loginResult.getUser() == null) {
                unauthorized(httpResponse, "Invalid access token");
                return;
            }
            httpRequest.setAttribute(ATTRIBUTE_USER, loginResult.getUser());
        } catch (Exception e) {
            unauthorized(httpResponse, "Invalid access token");
            return;
        }

        chain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String safeMessage = message.replace("\"", "");
        response.setHeader(
                "WWW-Authenticate", "Bearer error=\"invalid_token\", error_description=\"" + safeMessage + "\"");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"unauthorized\",\"error_description\":\"" + safeMessage + "\"}");
    }
}
