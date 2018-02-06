package org.traccar.database;

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.FCMPushNotificationType;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FCMPushNotificationTypeManager extends ExtendedObjectManager<FCMPushNotificationType> {

    private static Map<String, Long> fcmPushNotificationTypeToIdMap = new ConcurrentHashMap<>();
    private static Map<String, String> fcmPushNotificationTypeToStringMap = new ConcurrentHashMap<>();

    protected FCMPushNotificationTypeManager(DataManager dataManager) throws SQLException {
        super(dataManager, FCMPushNotificationType.class);
        refreshFCMNotificationTypesMap();
    }

    public static void refreshFCMNotificationTypesMap() {
        try {
            Collection<FCMPushNotificationType> fcmPushNotificationTypes =
                    Context.getDataManager().getFCMPushNotificationTypes();

            for (FCMPushNotificationType fcmPushNotificationType : fcmPushNotificationTypes) {
                fcmPushNotificationTypeToIdMap.put(fcmPushNotificationType.getEventType(),
                        fcmPushNotificationType.getId());
                fcmPushNotificationTypeToStringMap.put(fcmPushNotificationType.getEventType(),
                        fcmPushNotificationType.getNotificationString());
            }
        } catch (SQLException error) {
            Log.warning(error);
        }
    }

    public static Map<String, Long> getFCMPushNotificationStringToIdMap() {
        if (fcmPushNotificationTypeToStringMap.isEmpty()) {
            refreshFCMNotificationTypesMap();
        }
        return fcmPushNotificationTypeToIdMap;
    }

    public static Map<String, String> getFcmPushNotificationTypeToStringMap() {
        return fcmPushNotificationTypeToStringMap;
    }
}
