/*
 * Copyright 2015 - 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.traccar.Context;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

public class CorsResponseFilter implements ContainerResponseFilter {

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN_KEY = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN_VALUE = "*";

    public static final String ACCESS_CONTROL_ALLOW_HEADERS_KEY = "Access-Control-Allow-Headers";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS_VALUE = "origin, content-type, accept, authorization";

    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS_KEY = "Access-Control-Allow-Credentials";
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS_VALUE = "true";

    public static final String ACCESS_CONTROL_ALLOW_METHODS_KEY = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_METHODS_VALUE = "GET, POST, PUT, DELETE, OPTIONS";

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        if (!response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_HEADERS_KEY)) {
            response.getHeaders().add(ACCESS_CONTROL_ALLOW_HEADERS_KEY, ACCESS_CONTROL_ALLOW_HEADERS_VALUE);
        }
        if (!response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_CREDENTIALS_KEY)) {
            response.getHeaders().add(ACCESS_CONTROL_ALLOW_CREDENTIALS_KEY, ACCESS_CONTROL_ALLOW_CREDENTIALS_VALUE);
        }
        if (!response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_METHODS_KEY)) {
            response.getHeaders().add(ACCESS_CONTROL_ALLOW_METHODS_KEY, ACCESS_CONTROL_ALLOW_METHODS_VALUE);
        }

        if (!response.getHeaders().containsKey(ACCESS_CONTROL_ALLOW_ORIGIN_KEY)) {
            String origin = request.getHeaderString(HttpHeaders.Names.ORIGIN);
            String allowed = Context.getConfig().getString("web.origin");

            if (allowed == null || origin == null) {
                response.getHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN_KEY, ACCESS_CONTROL_ALLOW_ORIGIN_VALUE);
            } else if (allowed.equals(ACCESS_CONTROL_ALLOW_ORIGIN_VALUE) || allowed.contains(origin)) {
                response.getHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN_KEY, origin);
            }
        }
    }

}
