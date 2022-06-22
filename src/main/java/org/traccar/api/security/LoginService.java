/*
 * Copyright 2022 Anton Tananaev (anton@traccar.org)
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

import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.LdapProvider;
import org.traccar.model.User;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class LoginService {

    private final Storage storage;
    private final LdapProvider ldapProvider;

    private final String serviceAccountToken;
    private final boolean forceLdap;

    @Inject
    public LoginService(Config config, Storage storage, @Nullable LdapProvider ldapProvider) {
        this.storage = storage;
        this.ldapProvider = ldapProvider;
        serviceAccountToken = config.getString(Keys.WEB_SERVICE_ACCOUNT_TOKEN);
        forceLdap = config.getBoolean(Keys.LDAP_FORCE);
    }

    public User login(String token) throws StorageException {
        if (serviceAccountToken != null && serviceAccountToken.equals(token)) {
            return new ServiceAccountUser();
        }
        User user = storage.getObject(User.class, new Request(
                new Columns.All(), new Condition.Equals("token", "token", token)));
        if (user != null) {
            checkUserEnabled(user);
        }
        return user;
    }

    public User login(String email, String password) throws StorageException {
        User user = storage.getObject(User.class, new Request(
                new Columns.All(),
                new Condition.Or(
                        new Condition.Equals("email", "email", email.trim()),
                        new Condition.Equals("login", "email"))));
        if (user != null) {
            if (ldapProvider != null && user.getLogin() != null && ldapProvider.login(user.getLogin(), password)
                    || !forceLdap && user.isPasswordValid(password)) {
                checkUserEnabled(user);
                return user;
            }
        } else {
            if (ldapProvider != null && ldapProvider.login(email, password)) {
                user = ldapProvider.getUser(email);
                user.setId(storage.addObject(user, new Request(new Columns.Exclude("id"))));
                checkUserEnabled(user);
                return user;
            }
        }
        return null;
    }

    private void checkUserEnabled(User user) throws SecurityException {
        if (user == null) {
            throw new SecurityException("Unknown account");
        }
        if (user.getDisabled()) {
            throw new SecurityException("Account is disabled");
        }
        if (user.getExpirationTime() != null && System.currentTimeMillis() > user.getExpirationTime().getTime()) {
            throw new SecurityException("Account has expired");
        }
    }

}
