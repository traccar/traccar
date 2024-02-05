/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
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

import io.netty.handler.codec.http.HttpHeaderNames;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

@Singleton
public class CorsResponseFilter implements ContainerResponseFilter {

    private final String allowed;

    @Inject
    public CorsResponseFilter(Config config) {
        allowed = config.getString(Keys.WEB_ORIGIN);
    }

    private static final String ORIGIN_ALL = "*";
    private static final String HEADERS_ALL = "origin, content-type, accept, authorization";
    private static final String METHODS_ALL = "GET, POST, PUT, DELETE, OPTIONS";

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        if (!response.getHeaders().containsKey(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS.toString())) {
            response.getHeaders().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS.toString(), HEADERS_ALL);
        }

        if (!response.getHeaders().containsKey(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString())) {
            response.getHeaders().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString(), true);
        }

        if (!response.getHeaders().containsKey(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString())) {
            response.getHeaders().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString(), METHODS_ALL);
        }

        if (!response.getHeaders().containsKey(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString())) {
            String origin = request.getHeaderString(HttpHeaderNames.ORIGIN.toString());
            if (origin == null) {
                response.getHeaders().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString(), ORIGIN_ALL);
            } else if (allowed == null || allowed.equals(ORIGIN_ALL) || allowed.contains(origin)) {
                response.getHeaders().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString(), origin);
            }
        }
    }

}
