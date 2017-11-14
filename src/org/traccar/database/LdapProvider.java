/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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

import org.traccar.Config;
import org.traccar.helper.Log;
import org.traccar.model.User;

import java.util.Hashtable;

public class LdapProvider {

    private String url;
    private String searchBase;
    private String idAttribute;
    private String nameAttribute;
    private String mailAttribute;
    private String searchFilter;
    private String adminFilter;
    private String serviceUser;
    private String servicePassword;

    public LdapProvider(Config config) {
        String url = config.getString("ldap.url");
        if (url != null) {
            this.url = url;
        } else {
            this.url = "ldap://" + config.getString("ldap.server") + ":" + config.getInteger("ldap.port", 389);
        }
        this.searchBase = config.getString("ldap.base");
        this.idAttribute = config.getString("ldap.idAttribute", "uid");
        this.nameAttribute = config.getString("ldap.nameAttribute", "cn");
        this.mailAttribute = config.getString("ldap.mailAttribute", "mail");
        this.searchFilter = config.getString("ldap.searchFilter", "(" + idAttribute + "=:login)");
        String adminGroup = config.getString("ldap.adminGroup");
        this.adminFilter = config.getString("ldap.adminFilter");
        if (this.adminFilter == null && adminGroup != null) {
            this.adminFilter = "(&(" + idAttribute + "=:login)(memberOf=" + adminGroup + "))";
        }
        this.serviceUser = config.getString("ldap.user");
        this.servicePassword = config.getString("ldap.password");
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
                String searchString = adminFilter.replace(":login", accountName);
                SearchControls searchControls = new SearchControls();
                searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                NamingEnumeration<SearchResult> results = context.search(searchBase, searchString, searchControls);
                if (results.hasMoreElements()) {
                    results.nextElement();
                    if (results.hasMoreElements()) {
                        Log.warning("Matched multiple users for the accountName: " + accountName);
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

        String searchString = searchFilter.replace(":login", accountName);

        SearchControls searchControls = new SearchControls();
        String[] attributeFilter = {idAttribute, nameAttribute, mailAttribute};
        searchControls.setReturningAttributes(attributeFilter);
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration<SearchResult> results = context.search(searchBase, searchString, searchControls);

        SearchResult searchResult = null;
        if (results.hasMoreElements()) {
            searchResult = (SearchResult) results.nextElement();
            if (results.hasMoreElements()) {
                Log.warning("Matched multiple users for the accountName: " + accountName);
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
            user.setAdmin(isAdmin(accountName));
        } catch (NamingException e) {
            user.setLogin(accountName);
            user.setName(accountName);
            user.setEmail(accountName);
            Log.warning(e);
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

}
