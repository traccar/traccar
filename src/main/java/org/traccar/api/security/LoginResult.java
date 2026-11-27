package org.traccar.api.security;

import org.traccar.model.User;

import java.util.Date;

public class LoginResult {

    private final User user;
    private final Date expiration;
    private final Long tokenId;

    public LoginResult(User user) {
        this(user, null, null);
    }

    public LoginResult(User user, Date expiration, Long tokenId) {
        this.user = user;
        this.expiration = expiration;
        this.tokenId = tokenId;
    }

    public User getUser() {
        return user;
    }

    public Date getExpiration() {
        return expiration;
    }

    public Long getTokenId() {
        return tokenId;
    }

}
