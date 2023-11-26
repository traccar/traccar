package org.traccar.api.security;

import org.traccar.model.User;

import java.util.Date;

public class LoginResult {

    private final User user;
    private final Date expiration;

    public LoginResult(User user) {
        this.user = user;
        expiration = null;
    }

    public User getUser() {
        return user;
    }

    public Date getExpiration() {
        return expiration;
    }

}
