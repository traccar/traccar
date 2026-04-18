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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.codec.binary.Base64;
import org.traccar.api.signature.CryptoManager;
import org.traccar.api.signature.TokenManager;
import org.traccar.config.Config;
import org.traccar.helper.WebHelper;
import org.traccar.model.User;
import org.traccar.storage.StorageException;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
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

    public record AuthorizationCode(
            long userId,
            String clientId,
            URI redirectUri,
            String scope,
            Instant expiration,
            String nonce,
            String codeChallenge,
            String codeChallengeMethod) {
    }

    public static final JWSAlgorithm ID_TOKEN_ALGORITHM = JWSAlgorithm.ES256;
    private static final Duration DEFAULT_LIFETIME = Duration.ofMinutes(5);

    private final Config config;
    private final CryptoManager cryptoManager;
    private volatile ECKey signingKey;

    private final ConcurrentMap<String, AuthorizationCode> codes = new ConcurrentHashMap<>();

    @Inject
    public OidcSessionManager(Config config, CryptoManager cryptoManager) {
        this.config = config;
        this.cryptoManager = cryptoManager;
    }

    public String issueCode(
            long userId,
            String clientId,
            URI redirectUri,
            String scope,
            String nonce,
            String codeChallenge,
            String codeChallengeMethod) {
        byte[] random = new byte[32];
        ThreadLocalRandom.current().nextBytes(random);
        String code = Base64.encodeBase64URLSafeString(random);
        codes.put(code, new AuthorizationCode(
                userId, Objects.requireNonNull(clientId), redirectUri,
                scope == null || scope.isBlank() ? "openid" : scope,
                Instant.now().plus(DEFAULT_LIFETIME), nonce, codeChallenge, codeChallengeMethod));
        return code;
    }

    public String createAuthRequest(
            String clientId,
            URI redirectUri,
            String state,
            String scope,
            String nonce,
            String codeChallenge,
            String codeChallengeMethod) throws GeneralSecurityException, JOSEException, StorageException {

        ECKey key = getSigningKey();

        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(WebHelper.retrieveWebUrl(config) + "/api/oidc")
                .expirationTime(Date.from(Instant.now().plus(DEFAULT_LIFETIME)))
                .claim("client_id", clientId);

        if (redirectUri != null) {
            claims.claim("redirect_uri", redirectUri.toString());
        }
        if (state != null) {
            claims.claim("state", state);
        }
        if (scope != null) {
            claims.claim("scope", scope);
        }
        if (nonce != null) {
            claims.claim("nonce", nonce);
        }
        if (codeChallenge != null) {
            claims.claim("code_challenge", codeChallenge);
        }
        if (codeChallengeMethod != null) {
            claims.claim("code_challenge_method", codeChallengeMethod);
        }

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(ID_TOKEN_ALGORITHM)
                        .type(JOSEObjectType.JWT)
                        .keyID(key.getKeyID())
                        .build(),
                claims.build());
        jwt.sign(new ECDSASigner(key));
        return jwt.serialize();
    }

    public JWTClaimsSet getAuthRequest(String token) {
        if (token == null) {
            return null;
        }
        try {
            SignedJWT jwt = SignedJWT.parse(token);

            if (!jwt.getHeader().getAlgorithm().equals(ID_TOKEN_ALGORITHM)) {
                return null;
            }

            ECKey key = getSigningKey();
            if (!jwt.verify(new ECDSAVerifier(key.toECPublicKey()))) {
                return null;
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            Date expirationTime = claims.getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                return null;
            }

            return claims;
        } catch (GeneralSecurityException | JOSEException | StorageException | ParseException e) {
            return null;
        }
    }

    public AuthorizationCode consumeCode(String code, String clientId, URI redirectUri, String codeVerifier) {
        AuthorizationCode data = codes.remove(code);
        if (data == null) {
            return null;
        }
        if (!data.clientId().equals(clientId)) {
            return null;
        }
        if (data.redirectUri() != null) {
            if (redirectUri == null || !data.redirectUri().equals(redirectUri)) {
                return null;
            }
        }
        if (Instant.now().isAfter(data.expiration())) {
            return null;
        }
        if (!verifyCodeChallenge(data, codeVerifier)) {
            return null;
        }
        return data;
    }

    public String generateIdToken(
            AuthorizationCode authCode,
            String clientId,
            TokenManager.TokenData tokenData,
            Set<String> scopes,
            User user) throws GeneralSecurityException, JOSEException, StorageException {

        ECKey key = getSigningKey();
        Instant issuedAt = Instant.now();

        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(WebHelper.retrieveWebUrl(config) + "/api/oidc")
                .subject(String.valueOf(authCode.userId()))
                .audience(clientId)
                .expirationTime(tokenData.getExpiration())
                .issueTime(Date.from(issuedAt));

        if (authCode.nonce() != null) {
            claims.claim("nonce", authCode.nonce());
        }

        if (scopes.contains("email") || scopes.contains("profile")) {
            claims.claim("email", user.getEmail());
            claims.claim("name", user.getName());
        }

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(ID_TOKEN_ALGORITHM)
                        .type(JOSEObjectType.JWT)
                        .keyID(key.getKeyID())
                        .build(),
                claims.build());
        jwt.sign(new ECDSASigner(key));
        return jwt.serialize();
    }

    public Set<String> parseScopes(String scope) {
        return scope == null ? Set.of() : Stream.of(scope.split("\\s+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }

    public Map<String, Object> getJwks() throws GeneralSecurityException, StorageException, JOSEException {
        ECKey key = getSigningKey();
        return Map.of("keys", List.of(key.toPublicJWK().toJSONObject()));
    }

    private boolean verifyCodeChallenge(AuthorizationCode data, String codeVerifier) {
        if (data.codeChallenge() == null) {
            return true;
        }
        if (codeVerifier == null) {
            return false;
        }
        String method = data.codeChallengeMethod() == null ? "plain" : data.codeChallengeMethod();
        if ("S256".equalsIgnoreCase(method)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
                String computed = Base64.encodeBase64URLSafeString(hash);
                return computed.equals(data.codeChallenge());
            } catch (GeneralSecurityException e) {
                return false;
            }
        } else {
            return codeVerifier.equals(data.codeChallenge());
        }
    }

    private ECKey getSigningKey() throws GeneralSecurityException, StorageException, JOSEException {
        ECKey key = signingKey;
        if (key == null) {
            synchronized (this) {
                key = signingKey;
                if (key == null) {
                    KeyPair keyPair = cryptoManager.getKeyPair();
                    ECKey jwk = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
                            .privateKey((ECPrivateKey) keyPair.getPrivate())
                            .keyUse(KeyUse.SIGNATURE)
                            .algorithm(ID_TOKEN_ALGORITHM)
                            .keyIDFromThumbprint()
                            .build();
                    signingKey = jwk;
                    key = jwk;
                }
            }
        }
        return key;
    }

}
