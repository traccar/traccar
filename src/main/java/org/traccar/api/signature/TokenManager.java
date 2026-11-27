/*
 * Copyright 2022 - 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.api.signature;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.traccar.model.RevokedToken;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Singleton
public class TokenManager {

    private static final int DEFAULT_EXPIRATION_DAYS = 7;

    private final ObjectMapper objectMapper;
    private final CryptoManager cryptoManager;
    private final Storage storage;

    private final SecureRandom random = new SecureRandom();

    public static class TokenData {
        @JsonProperty("i")
        private long id;
        @JsonProperty("u")
        private long userId;
        @JsonProperty("e")
        private Date expiration;
        @JsonProperty("p")
        private TokenPurpose purpose;

        public long getId() {
            return id;
        }

        public long getUserId() {
            return userId;
        }

        public Date getExpiration() {
            return expiration;
        }

        public TokenPurpose getPurpose() {
            return purpose != null ? purpose : TokenPurpose.ACCESS;
        }
    }

    @Inject
    public TokenManager(ObjectMapper objectMapper, CryptoManager cryptoManager, Storage storage) {
        this.objectMapper = objectMapper;
        this.cryptoManager = cryptoManager;
        this.storage = storage;
    }

    public String generateToken(long userId, Date expiration, TokenPurpose purpose)
            throws IOException, GeneralSecurityException, StorageException {
        TokenData data = new TokenData();
        data.userId = userId;
        data.purpose = purpose;
        data.id = random.nextLong() & Long.MAX_VALUE;
        if (expiration != null) {
            data.expiration = expiration;
        } else {
            data.expiration = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(DEFAULT_EXPIRATION_DAYS));
        }
        byte[] encoded = objectMapper.writeValueAsBytes(data);
        return Base64.encodeBase64URLSafeString(cryptoManager.sign(encoded));
    }

    public TokenData verifyToken(String token, TokenPurpose purpose)
            throws IOException, GeneralSecurityException, StorageException {
        TokenData data = decodeToken(token);
        if (data.getPurpose() != TokenPurpose.ACCESS && data.getPurpose() != purpose) {
            throw new SecurityException("Invalid token purpose");
        }
        if (data.expiration.before(new Date())) {
            throw new SecurityException("Token has expired");
        }
        checkRevoked(data.getId());
        return data;
    }

    public void checkRevoked(long tokenId) throws StorageException {
        var revoked = storage.getObject(RevokedToken.class, new Request(
                new Columns.All(), new Condition.Equals("id", tokenId)));
        if (revoked != null) {
            throw new SecurityException("Token has been revoked");
        }
    }

    public TokenData decodeToken(String token) throws IOException, GeneralSecurityException, StorageException {
        byte[] encoded = cryptoManager.verify(Base64.decodeBase64(token));
        return objectMapper.readValue(encoded, TokenData.class);
    }

}
