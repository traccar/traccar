package org.traccar.database;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.traccar.Context;
import org.traccar.fcm.PushNotifications;
import org.traccar.helper.Log;
import org.traccar.model.Event;
import org.traccar.model.FCMNotification;
import org.traccar.model.Position;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FCMPushNotificationManager extends ExtendedObjectManager<FCMNotification> {

    public static final long DEFAULT_REFRESH_DELAY = 300;

    private final Map<String, String> fcmNotificationTokens = new ConcurrentHashMap<>();

    public FCMPushNotificationManager(DataManager dataManager) {
        super(dataManager, FCMNotification.class);
        refreshFCMNotificationItems();
    }

    public void refreshFCMNotificationItems() {
        if (getDataManager() != null) {
            try {
                for (FCMNotification fcmNotification
                        : getDataManager().getFCMNotifications()) {
                    String key = getTokenLookupKey(
                            fcmNotification.getDeviceId(),
                            fcmNotification.getUserId(),
                            fcmNotification.getEventType());

                    fcmNotificationTokens.put(key, fcmNotification.getClientToken());
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }

    private String getTokenLookupKey(long deviceId, long userId, String eventType) {
        return String.format("%d_%d_%s", deviceId, userId, eventType);
    }

    public void updateEvents(Map<Event, Position> events) {
        for (Map.Entry<Event, Position> event : events.entrySet()) {
            updateEvent(event.getKey(), event.getValue());
        }
    }

    public void updateEvent(Event event, Position position) {

        String eventType = event.getType();
        if (StringUtils.isBlank(eventType)) {
            return;
        }

        long deviceId = event.getDeviceId();
        Set<Long> userIds = Context.getPermissionsManager().getDeviceUsers(deviceId);

        Set<String> tokens = new ConcurrentHashSet<>();

        for (long userId : userIds) {
            String tokenLookupKey = getTokenLookupKey(deviceId, userId, eventType);
            tokens.add(fcmNotificationTokens.get(tokenLookupKey));
        }

        String title = eventType + " by " + deviceId;
        String body = eventType + " detected on device " + deviceId + " at " + position.getDeviceTime();
        PushNotifications.getInstance().sendEventNotification(tokens, title, body);
    }
}
