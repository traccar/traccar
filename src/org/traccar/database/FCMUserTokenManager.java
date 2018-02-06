package org.traccar.database;

import org.traccar.model.FCMUserToken;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FCMUserTokenManager extends ExtendedObjectManager<FCMUserToken> {

    private final Map<Long, String> fcmNotificationTokens = new ConcurrentHashMap<>();

    public FCMUserTokenManager(DataManager dataManager) {
        super(dataManager, FCMUserToken.class);
        refreshFCMUserTokens();
    }

    public void refreshFCMUserTokens() {
        if (getDataManager() != null) {
            try {
                Collection<FCMUserToken> tokensByUser = getDataManager().getFCMUserTokens();
                for (FCMUserToken userToken : tokensByUser) {
                    fcmNotificationTokens.put(userToken.getUserId(), userToken.getClientToken());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Optional<String> getFCMUserToken(long userId) {

        if (fcmNotificationTokens.containsKey(userId)) {
            return Optional.of(fcmNotificationTokens.get(userId));
        }

        return Optional.empty();
    }
}
