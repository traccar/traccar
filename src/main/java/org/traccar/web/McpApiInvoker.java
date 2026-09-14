/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;
import org.traccar.api.ApiResourceConfig;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Singleton
public class McpApiInvoker {

    private volatile ApplicationHandler applicationHandler;

    private ApplicationHandler getApplicationHandler() {
        ApplicationHandler result = applicationHandler;
        if (result == null) {
            synchronized (this) {
                result = applicationHandler;
                if (result == null) {
                    ResourceConfig resourceConfig = ApiResourceConfig.create();
                    result = new ApplicationHandler(resourceConfig);
                    applicationHandler = result;
                }
            }
        }
        return result;
    }

    public String get(String path, Map<String, Object> arguments, String authorization) {
        URI baseUri = URI.create("http://mcp-loopback.invalid/api/");
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUri).path(path);
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            List<?> values = entry.getValue() instanceof List<?> list ? list : List.of(entry.getValue());
            uriBuilder.queryParam(entry.getKey(), values.toArray());
        }
        ContainerRequest containerRequest = new ContainerRequest(
                baseUri, uriBuilder.build(), "GET", null, new MapPropertiesDelegate(), null);
        containerRequest.header(HttpHeaders.AUTHORIZATION, authorization);
        containerRequest.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        try {
            ContainerResponse response = getApplicationHandler().apply(containerRequest, responseBody).get();
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                throw new RuntimeException(
                        "Request failed with status " + response.getStatus() + ": "
                                + responseBody.toString(StandardCharsets.UTF_8));
            }
            return responseBody.toString(StandardCharsets.UTF_8);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
