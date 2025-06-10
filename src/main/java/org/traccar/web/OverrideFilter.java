/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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

import com.google.inject.Provider;
import org.traccar.api.security.PermissionsService;
import org.traccar.model.Server;
import org.traccar.storage.StorageException;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Singleton
public class OverrideFilter implements Filter {

    private final Provider<PermissionsService> permissionsServiceProvider;

    @Inject
    public OverrideFilter(Provider<PermissionsService> permissionsServiceProvider) {
        this.permissionsServiceProvider = permissionsServiceProvider;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (((HttpServletRequest) request).getServletPath().startsWith("/api")) {
            chain.doFilter(request, response);
            return;
        }

        ResponseWrapper wrappedResponse = new ResponseWrapper((HttpServletResponse) response);

        chain.doFilter(request, wrappedResponse);

        byte[] bytes = wrappedResponse.getCapture();
        if (bytes != null) {
            if (wrappedResponse.getContentType() != null && wrappedResponse.getContentType().contains("text/html")
                    || ((HttpServletRequest) request).getPathInfo().endsWith("manifest.webmanifest")) {

                Server server;
                try {
                    server = permissionsServiceProvider.get().getServer();
                } catch (StorageException e) {
                    throw new RuntimeException(e);
                }

                String title = server.getString("title", "Traccar");
                String description = server.getString("description", "Traccar GPS Tracking System");
                String colorPrimary = server.getString("colorPrimary", "#1a237e");

                String alteredContent = new String(wrappedResponse.getCapture(), StandardCharsets.UTF_8)
                        .replace("${title}", title)
                        .replace("${description}", description)
                        .replace("${colorPrimary}", colorPrimary);

                byte[] data = alteredContent.getBytes(StandardCharsets.UTF_8);
                response.setContentLength(data.length);
                response.getOutputStream().write(data);

            } else {
                response.getOutputStream().write(bytes);
            }
        }
    }

}
