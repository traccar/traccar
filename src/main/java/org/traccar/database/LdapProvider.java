/*
 * Copyright 2017 - 2020 Anton Tananaev (anton@traccar.org)
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
package org.traccar.database;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.User;

import java.util.Hashtable;

public class LdapProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapProvider.class);

    private final String url;
    private final String searchBase;
    private final String idAttribute;
    private final String nameAttribute;
    private final String mailAttribute;
    private final String searchFilter;
    private final String adminFilter;
    private final String serviceUser;
    private final String servicePassword;

    public LdapProvider(Config config) {
        url = config.getString(Keys.LDAP_URL);
        searchBase = config.getString(Keys.LDAP_BASE);
        idAttribute = config.getString(Keys.LDAP_ID_ATTRIBUTE);
        nameAttribute = config.getString(Keys.LDAP_NAME_ATTRIBUTE);
        mailAttribute = config.getString(Keys.LDAP_MAIN_ATTRIBUTE);
        if (config.hasKey(Keys.LDAP_SEARCH_FILTER)) {
            searchFilter = config.getString(Keys.LDAP_SEARCH_FILTER);
        } else {
            searchFilter = "(" + idAttribute + "=:login)";
        }
        if (config.hasKey(Keys.LDAP_ADMIN_FILTER)) {
            adminFilter = config.getString(Keys.LDAP_ADMIN_FILTER);
        } else {
            String adminGroup = config.getString(Keys.LDAP_ADMIN_GROUP);
            if (adminGroup != null) {
                adminFilter = "(&(" + idAttribute + "=:login)(memberOf=" + adminGroup + "))";
            } else {
                adminFilter = null;
            }
        }
        serviceUser = config.getString(Keys.LDAP_USER);
        servicePassword = config.getString(Keys.LDAP_PASSWORD);
    }

    private InitialDirContext auth(String accountName, String password) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);

        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, accountName);
        env.put(Context.SECURITY_CREDENTIALS, password);

        return new InitialDirContext(env);
    }

    private boolean isAdmin(String accountName) {
        if (this.adminFilter != null) {
            try {
                InitialDirContext context = initContext();
                String searchString = adminFilter.replace(":login", encodeForLdap(accountName));
                SearchControls searchControls = new SearchControls();
                searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                NamingEnumeration<SearchResult> results = context.search(searchBase, searchString, searchControls);
                if (results.hasMoreElements()) {
                    results.nextElement();
                    if (results.hasMoreElements()) {
                        LOGGER.warn("Matched multiple users for the accountName: " + accountName);
                        return false;
                    }
                    return true;
                }
            } catch (NamingException e) {
                return false;
            }
        }
        return false;
    }

    public InitialDirContext initContext() throws NamingException {
        return auth(serviceUser, servicePassword);
    }

    private SearchResult lookupUser(String accountName) throws NamingException {
        InitialDirContext context = initContext();

        String searchString = searchFilter.replace(":login", encodeForLdap(accountName));

        SearchControls searchControls = new SearchControls();
        String[] attributeFilter = {idAttribute, nameAttribute, mailAttribute};
        searchControls.setReturningAttributes(attributeFilter);
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration<SearchResult> results = context.search(searchBase, searchString, searchControls);

        SearchResult searchResult = null;
        if (results.hasMoreElements()) {
            searchResult = results.nextElement();
            if (results.hasMoreElements()) {
                LOGGER.warn("Matched multiple users for the accountName: " + accountName);
                return null;
            }
        }

        return searchResult;
    }

    public User getUser(String accountName) {
        SearchResult ldapUser;
        User user = new User();
        try {
            ldapUser = lookupUser(accountName);
            if (ldapUser != null) {
                Attribute attribute = ldapUser.getAttributes().get(idAttribute);
                if (attribute != null) {
                    user.setLogin((String) attribute.get());
                } else {
                    user.setLogin(accountName);
                }
                attribute = ldapUser.getAttributes().get(nameAttribute);
                if (attribute != null) {
                    user.setName((String) attribute.get());
                } else {
                    user.setName(accountName);
                }
                attribute = ldapUser.getAttributes().get(mailAttribute);
                if (attribute != null) {
                    user.setEmail((String) attribute.get());
                } else {
                    user.setEmail(accountName);
                }
            }
            user.setAdministrator(isAdmin(accountName));
        } catch (NamingException e) {
            user.setLogin(accountName);
            user.setName(accountName);
            user.setEmail(accountName);
            LOGGER.warn("User lookup error", e);
        }
        return user;
    }

    public boolean login(String username, String password) {
        try {
            SearchResult ldapUser = lookupUser(username);
            if (ldapUser != null) {
                auth(ldapUser.getNameInNamespace(), password).close();
                return true;
            }
        } catch (NamingException e) {
            return false;
        }
        return false;
    }

    public String encodeForLdap(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\5c");
                    break;
                case '*':
                    sb.append("\\2a");
                    break;
                case '(':
                    sb.append("\\28");
                    break;
                case ')':
                    sb.append("\\29");
                    break;
                case '\0':
                    sb.append("\\00");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

}
