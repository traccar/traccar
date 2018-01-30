package org.traccar.model;

public class FCMUserToken extends BaseModel {

    private long userId;
    private String clientToken;

    public long getUserId() { return userId; }

    public void setUserId(long userId) { this.userId = userId; }

    public String getClientToken() { return clientToken; }

    public void setClientToken(String clientToken) { this.clientToken = clientToken; }
}
