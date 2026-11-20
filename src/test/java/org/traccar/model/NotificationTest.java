package org.traccar.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NotificationTest {
    @Test
    @DisplayName("Notification: if null return empty set")
    public void ifNullReturnEmptySet() {
        Notification notification = new Notification();
        notification.setNotificators(null);
        assertEquals(Set.of(), notification.getNotificatorsTypes());
    }

    @Test
    @DisplayName("Notification: splits and trims notificators")
    public void splitsAndTrimsNotificators() {
        Notification notification = new Notification();
        notification.setNotificators("web, mail , sms"); // Yes, space after mail is meant to test the trim
        assertEquals(Set.of("web", "mail", "sms"), notification.getNotificatorsTypes());
    }

    @Test
    @DisplayName("Notification: duplicate types become one")
    public void duplicateNotificationTypesBecomeOne() {
        Notification notification = new Notification();
        notification.setNotificators("web, web");
        assertEquals(Set.of("web"), notification.getNotificatorsTypes());
    }
}
