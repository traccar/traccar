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
package org.traccar.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.traccar.api.signature.TokenManager;
import org.traccar.config.Config;
import org.traccar.helper.WebHelper;
import org.traccar.model.User;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64.Encoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class OidcSessionManager {

    public static class AuthorizationCode {

        private final long userId;
        private final String clientId;
        private final URI redirectUri;
        private final String scope;
        private final Instant expiration;

        AuthorizationCode(long userId, String clientId, URI redirectUri, String scope, Instant expiration) {
            this.userId = userId;
            this.clientId = clientId;
            this.redirectUri = redirectUri;
            this.scope = scope;
            this.expiration = expiration;
        }

        public long getUserId() {
            return userId;
        }

        public String getClientId() {
            return clientId;
        }

        public URI getRedirectUri() {
            return redirectUri;
        }

        public String getScope() {
            return scope;
        }

        public Instant getExpiration() {
            return expiration;
        }
    }

    private static final Duration DEFAULT_LIFETIME = Duration.ofMinutes(5);

    private final Config config;
    private final ObjectMapper objectMapper;

    private final ConcurrentMap<String, AuthorizationCode> codes = new ConcurrentHashMap<>();

    @Inject
    public OidcSessionManager(Config config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public String issueCode(long userId, String clientId, URI redirectUri, String scope) {
        byte[] random = new byte[32];
        ThreadLocalRandom.current().nextBytes(random);
        String code = Base64.encodeBase64URLSafeString(random);
        codes.put(code, new AuthorizationCode(
                userId, Objects.requireNonNull(clientId), redirectUri,
                scope == null || scope.isBlank() ? "openid" : scope,
                Instant.now().plus(DEFAULT_LIFETIME)));
        return code;
    }

    public AuthorizationCode consumeCode(String code, String clientId, URI redirectUri) {
        AuthorizationCode data = codes.remove(code);
        if (data == null) {
            return null;
        }
        if (!data.getClientId().equals(clientId)) {
            return null;
        }
        if (redirectUri != null && !data.getRedirectUri().equals(redirectUri)) {
            return null;
        }
        if (Instant.now().isAfter(data.getExpiration())) {
            return null;
        }
        return data;
    }

    public String generateIdToken(
            AuthorizationCode authCode,
            String clientId,
            TokenManager.TokenData tokenData,
            Set<String> scopes,
            User user) throws IOException {

        Map<String, Object> header = Map.of("alg", "none", "typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", WebHelper.retrieveWebUrl(config) + "/api/oidc");
        payload.put("sub", String.valueOf(authCode.getUserId()));
        payload.put("aud", clientId);
        payload.put("exp", tokenData.getExpiration().toInstant().getEpochSecond());
        payload.put("iat", Instant.now().getEpochSecond());

        if (scopes.contains("email") || scopes.contains("profile")) {
            payload.put("email", user.getEmail());
            payload.put("name", user.getName());
        }

        return encodeSegment(header) + "." + encodeSegment(payload) + ".";
    }

    public Set<String> parseScopes(String scope) {
        return scope == null ? Set.of() : Stream.of(scope.split("\\s+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }

    private String encodeSegment(Object data) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        objectMapper.writeValue(output, data);
        Encoder encoder = java.util.Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(output.toByteArray());
    }

}
