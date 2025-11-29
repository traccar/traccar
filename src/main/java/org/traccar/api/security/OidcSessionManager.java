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

import org.apache.commons.codec.binary.Base64;

import jakarta.inject.Singleton;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

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

    private final ConcurrentMap<String, AuthorizationCode> codes = new ConcurrentHashMap<>();

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
}
