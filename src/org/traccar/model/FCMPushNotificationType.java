package org.traccar.model;

public class FCMPushNotificationType extends BaseModel {

    private String eventType;

    public String getEventType() { return eventType; }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}
