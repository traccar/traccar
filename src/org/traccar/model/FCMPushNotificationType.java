package org.traccar.model;

public class FCMPushNotificationType extends BaseModel {

    private String eventType;
    private String prettyName;
    private String notificationString;

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

    public String getNotificationString() {
        return notificationString;
    }

    public void setNotificationString(String notificationString) {
        this.notificationString = notificationString;
    }
}
