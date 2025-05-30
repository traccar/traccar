/*
 * Copyright 2025 Haven Madray (sgpublic2002@gmail.com)
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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import liquibase.util.MD5Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class SignedRequestProvider extends TreeMap<String, Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SignedRequestProvider.class);

    private WebTarget request;
    private final String path;
    private final String secretKey;

    public SignedRequestProvider(String secretKey, Client client, String url) {
        this.request = client.target(url);
        this.path = request.getUri().getPath();
        this.secretKey = secretKey;
    }

    private String convertIfNeedUrlEncode(String value, boolean need) {
        if (need) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } else {
            return value;
        }
    }

    public Invocation.Builder requestWithSign(String signKeyName, boolean signWithPath,
                                              boolean signNeedEntireUrlEncode, boolean signNeedParamsUrlEncode) {
        if (secretKey != null) {
            StringBuilder queryString = new StringBuilder();

            if (signWithPath) {
                queryString.append(path);
                if (!isEmpty()) {
                    queryString.append("?");
                }
            }
            for (Map.Entry<String, ?> pair : entrySet()) {
                queryString.append(pair.getKey())
                        .append("=")
                        .append(convertIfNeedUrlEncode(pair.getValue().toString(), signNeedParamsUrlEncode))
                        .append("&");
                request = request.queryParam(pair.getKey(), pair.getValue());
            }

            if (queryString.charAt(queryString.length() - 1) == '&') {
                queryString.deleteCharAt(queryString.length() - 1);
            }

            queryString.append(secretKey);

            String sign = MD5Util.computeMD5(convertIfNeedUrlEncode(queryString.toString(), signNeedEntireUrlEncode));
            put(signKeyName, sign);
            request = request.queryParam(signKeyName, sign);
        }
        return request.request();
    }
}
