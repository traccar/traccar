/*
 * Copyright 2020 - 2026 Anton Tananaev (anton@traccar.org)
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
package org.traccar.helper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.Response;

import org.eclipse.jetty.util.URIUtil;
import org.traccar.config.Config;
import org.traccar.config.Keys;

public final class WebHelper {

    private static final List<String> PROXY_HEADERS = List.of("CF-Connecting-IP", "X-Real-IP", "X-Forwarded-For");

    private WebHelper() {}

    public static String retrieveRemoteAddress(HttpServletRequest request) {
        if (request != null) {
            for (String proxyHeader : PROXY_HEADERS) {
                String originalAddress = request.getHeader(proxyHeader);
                if (originalAddress != null && !originalAddress.isEmpty()) {
                    int delimiter = originalAddress.indexOf(",");
                    return delimiter > 0 ? originalAddress.substring(0, delimiter) : originalAddress;
                }
            }
            return request.getRemoteAddr();
        } else {
            return null;
        }
    }

    public static String retrieveWebUrl(Config config) {
        if (config.hasKey(Keys.WEB_URL)) {
            return config.getString(Keys.WEB_URL).replaceAll("/$", "");
        } else {
            String address;
            try {
                address = config.getString(Keys.WEB_ADDRESS, InetAddress.getLocalHost().getHostAddress());
            } catch (UnknownHostException e) {
                address = "localhost";
            }
            return URIUtil.newURI("http", address, config.getInteger(Keys.WEB_PORT));
        }
    }

    public static CompletableFuture<Void> post(
            Invocation.Builder request, Entity<?> entity, Consumer<Response> handler) {
        var future = new CompletableFuture<Void>();
        request.async().post(entity, new InvocationCallback<Response>() {
            @Override
            public void completed(Response response) {
                try (response) {
                    handler.accept(response);
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }

            @Override
            public void failed(Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

}
