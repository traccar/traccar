package org.traccar.model;

public class FCMPushNotificationType extends BaseModel {

    private String eventType;
    private String prettyName;

    public String getEventType() { return eventType; }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPrettyName() {
        return prettyName;
    }

    public void setPrettyName(String prettyName) {
        this.prettyName = prettyName;
    }
}
