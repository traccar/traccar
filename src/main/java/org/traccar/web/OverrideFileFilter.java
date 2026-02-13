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
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

public class OverrideFileFilter implements Filter {

    private Resource overrideBase;

    @Override
    public void init(FilterConfig config) {
        String overridePath = config.getInitParameter("overridePath");
        var context = ServletContextHandler.getServletContextHandler(config.getServletContext());
        overrideBase = ResourceFactory.of(context).newResource(new File(overridePath).toPath());
    }

    @Override
    public void doFilter(
            ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (!"GET".equals(httpRequest.getMethod()) && !"HEAD".equals(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String uri = httpRequest.getRequestURI();
        if (uri.startsWith("/api") || uri.startsWith("/console")) {
            chain.doFilter(request, response);
            return;
        }

        String path = uri.substring(httpRequest.getContextPath().length());
        if (path.isEmpty()) {
            path = "/";
        }

        Resource candidate = overrideBase.resolve(path);
        if (candidate != null && candidate.exists() && !candidate.isDirectory()) {
            request.getRequestDispatcher("/override" + path).forward(request, response);
            return;
        }

        String accept = httpRequest.getHeader("Accept");
        boolean acceptHtml = accept != null && accept.contains("text/html");
        String last = path.endsWith("/") ? "" : path.substring(path.lastIndexOf('/') + 1);
        boolean appRoute = path.endsWith("/") || !last.contains(".");

        if (acceptHtml && appRoute) {
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

}
