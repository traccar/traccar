package org.traccar.database;

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.FCMPushNotificationType;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FCMPushNotificationTypeManager extends ExtendedObjectManager<FCMPushNotificationType> {

    private static Map<Long, String> fcmPushNotificationTypesMap = new ConcurrentHashMap<>();
    private static Map<String, Long> fcmPushNotificationStringToIdMap = new ConcurrentHashMap<>();

    protected FCMPushNotificationTypeManager(DataManager dataManager) throws SQLException {
        super(dataManager, FCMPushNotificationType.class);
        refreshFCMNotificationTypesMap();
    }

    public static void refreshFCMNotificationTypesMap() {
        try {
            Collection<FCMPushNotificationType> fcmPushNotificationTypes =
                    Context.getDataManager().getFCMPushNotificationTypes();

            for (FCMPushNotificationType fcmPushNotificationType : fcmPushNotificationTypes) {
                fcmPushNotificationTypesMap.put(fcmPushNotificationType.getId(), fcmPushNotificationType.getEventType());
                fcmPushNotificationStringToIdMap.put(fcmPushNotificationType.getEventType(), fcmPushNotificationType.getId());
            }
        } catch (SQLException error) {
            Log.warning(error);
        }
    }

    public static Map<Long, String> getFcmPushNotificationTypesMap() {
        return fcmPushNotificationTypesMap;
    }

    public static Map<String, Long> getFCMPushNotificationStringToIdMap() {
        if (fcmPushNotificationTypesMap.isEmpty()) {
            refreshFCMNotificationTypesMap();
        }
        return fcmPushNotificationStringToIdMap;
    }
}
