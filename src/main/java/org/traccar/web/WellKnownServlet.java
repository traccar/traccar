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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.traccar.api.security.OidcSessionManager;
import org.traccar.config.Config;
import org.traccar.helper.WebHelper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class WellKnownServlet extends HttpServlet {

    private final Config config;
    private final ObjectMapper objectMapper;

    @Inject
    public WellKnownServlet(Config config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        Map<String, Object> payload = switch (path) {
            case "/openid-configuration" -> openIdConfiguration();
            case "/oauth-authorization-server" -> authorizationServerConfiguration();
            case "/oauth-protected-resource" -> protectedResourceConfiguration();
            default -> null;
        };
        if (payload != null) {
            resp.setContentType("application/json");
            objectMapper.writeValue(resp.getWriter(), payload);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private String issuer() {
        return WebHelper.retrieveWebUrl(config) + "/api/oidc";
    }

    private String mcpResource() {
        return WebHelper.retrieveWebUrl(config) + "/api/mcp";
    }

    private Map<String, Object> openIdConfiguration() {
        String issuer = issuer();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("issuer", issuer);
        payload.put("authorization_endpoint", issuer + "/authorize");
        payload.put("token_endpoint", issuer + "/token");
        payload.put("userinfo_endpoint", issuer + "/userinfo");
        payload.put("jwks_uri", issuer + "/jwks");
        payload.put("subject_types_supported", List.of("public"));
        payload.put("response_types_supported", List.of("code"));
        payload.put("grant_types_supported", List.of("authorization_code"));
        payload.put("scopes_supported", List.of("openid", "profile", "email"));
        payload.put("id_token_signing_alg_values_supported", List.of(OidcSessionManager.ID_TOKEN_ALGORITHM));
        payload.put("code_challenge_methods_supported", List.of("S256", "plain"));
        payload.put("token_endpoint_auth_methods_supported",
                List.of("client_secret_basic", "client_secret_post", "none"));
        return payload;
    }

    private Map<String, Object> authorizationServerConfiguration() {
        String issuer = issuer();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("issuer", issuer);
        payload.put("authorization_endpoint", issuer + "/authorize");
        payload.put("token_endpoint", issuer + "/token");
        payload.put("jwks_uri", issuer + "/jwks");
        payload.put("response_types_supported", List.of("code"));
        payload.put("grant_types_supported", List.of("authorization_code"));
        payload.put("scopes_supported", List.of("openid", "profile", "email"));
        payload.put("code_challenge_methods_supported", List.of("S256", "plain"));
        payload.put("token_endpoint_auth_methods_supported",
                List.of("client_secret_basic", "client_secret_post", "none"));
        return payload;
    }

    private Map<String, Object> protectedResourceConfiguration() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("issuer", issuer());
        payload.put("authorization_servers", List.of(issuer()));
        payload.put("jwks_uri", issuer() + "/jwks");
        payload.put("scopes_supported", List.of("openid", "profile", "email"));
        payload.put("resource", mcpResource());
        return payload;
    }

}
