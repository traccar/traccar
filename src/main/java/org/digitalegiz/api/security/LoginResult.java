package org.digitalegiz.api.security;

import org.digitalegiz.model.User;

import java.util.Date;

public class LoginResult {

    private final User user;
    private final Date expiration;

    public LoginResult(User user) {
        this(user, null);
    }

    public LoginResult(User user, Date expiration) {
        this.user = user;
        this.expiration = expiration;
    }

    public User getUser() {
        return user;
    }

    public Date getExpiration() {
        return expiration;
    }

}
