/*
 * Copyright 2022 - 2023 Anton Tananaev (anton@traccar.org)
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

import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.traccar.api.signature.TokenManager;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.LdapProvider;
import org.traccar.helper.model.UserUtil;
import org.traccar.model.User;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.security.GeneralSecurityException;

@Singleton
public class LoginService {

    private final Config config;
    private final Storage storage;
    private final TokenManager tokenManager;
    private final LdapProvider ldapProvider;

    private final String serviceAccountToken;
    private final boolean forceLdap;
    private final boolean forceOpenId;

    @Inject
    public LoginService(
            Config config, Storage storage, TokenManager tokenManager, @Nullable LdapProvider ldapProvider) {
        this.storage = storage;
        this.config = config;
        this.tokenManager = tokenManager;
        this.ldapProvider = ldapProvider;
        serviceAccountToken = config.getString(Keys.WEB_SERVICE_ACCOUNT_TOKEN);
        forceLdap = config.getBoolean(Keys.LDAP_FORCE);
        forceOpenId = config.getBoolean(Keys.OPENID_FORCE);
    }

    public LoginResult login(String token) throws StorageException, GeneralSecurityException, IOException {
        if (serviceAccountToken != null && serviceAccountToken.equals(token)) {
            return new LoginResult(new ServiceAccountUser());
        }
        TokenManager.TokenData tokenData = tokenManager.verifyToken(token);
        User user = storage.getObject(User.class, new Request(
                new Columns.All(), new Condition.Equals("id", tokenData.getUserId())));
        if (user != null) {
            checkUserEnabled(user);
        }
        return new LoginResult(user, tokenData.getExpiration());
    }

    public LoginResult login(String email, String password, Integer code) throws StorageException {
        if (forceOpenId) {
            return null;
        }

        email = email.trim();
        User user = storage.getObject(User.class, new Request(
                new Columns.All(),
                new Condition.Or(
                        new Condition.Equals("email", email),
                        new Condition.Equals("login", email))));
        if (user != null) {
            if (ldapProvider != null && user.getLogin() != null && ldapProvider.login(user.getLogin(), password)
                    || !forceLdap && user.isPasswordValid(password)) {
                checkUserCode(user, code);
                checkUserEnabled(user);
                return new LoginResult(user);
            }
        } else {
            if (ldapProvider != null && ldapProvider.login(email, password)) {
                user = ldapProvider.getUser(email);
                user.setId(storage.addObject(user, new Request(new Columns.Exclude("id"))));
                checkUserEnabled(user);
                return new LoginResult(user);
            }
        }
        return null;
    }

    public LoginResult login(String email, String name, boolean administrator) throws StorageException {
        User user = storage.getObject(User.class, new Request(
            new Columns.All(),
            new Condition.Equals("email", email)));

        if (user == null) {
            user = new User();
            UserUtil.setUserDefaults(user, config);
            user.setName(name);
            user.setEmail(email);
            user.setFixedEmail(true);
            user.setAdministrator(administrator);
            user.setId(storage.addObject(user, new Request(new Columns.Exclude("id"))));
        }
        checkUserEnabled(user);
        return new LoginResult(user);
    }

    private void checkUserEnabled(User user) throws SecurityException {
        if (user == null) {
            throw new SecurityException("Unknown account");
        }
        user.checkDisabled();
    }

    private void checkUserCode(User user, Integer code) throws SecurityException {
        String key = user.getTotpKey();
        if (key != null && !key.isEmpty()) {
            if (code == null) {
                throw new CodeRequiredException();
            }
            GoogleAuthenticator authenticator = new GoogleAuthenticator();
            if (!authenticator.authorize(key, code)) {
                throw new SecurityException("User authorization failed");
            }
        }
    }

}
