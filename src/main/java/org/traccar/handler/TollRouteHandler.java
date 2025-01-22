
// todo implement cache
package org.traccar.handler;

import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.database.NotificationManager;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.tollroute.TollData;
import org.traccar.tollroute.TollRouteProvider;

public class TollRouteHandler extends BasePositionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TollRouteHandler.class);
    private final NotificationManager notificationManager;
    private final TollRouteProvider tollRouteProvider;

    @Inject
    public TollRouteHandler( TollRouteProvider tollRouteProvider , NotificationManager notificationManager) {
        this.tollRouteProvider = tollRouteProvider;
        this.notificationManager = notificationManager;

    }

    @Override
    public void onPosition(Position position, Callback callback) {
        if (position.getValid()) {
            tollRouteProvider.getTollRoute(position.getLatitude(), position.getLongitude(),
                    new TollRouteProvider.TollRouteProviderCallback() {
                @Override
                public void onSuccess(TollData tollData) {
                    position.set(Position.KEY_TOLL_COST, tollData.getToll());
                    position.set(Position.KEY_TOLL_REF, tollData.getRef());
                    position.set(Position.KEY_TOLL_NAME, tollData.getName());
                            callback.processed(false);
                            Event event = new Event(Event.TYPE_TOLL_ROUTE, position.getDeviceId());
                            event.setPositionId(position.getId());

                                // takes in a map hence this type gymnastics
                                Map<Event, Position> updates = new HashMap<>();
                            updates.put(event, position);


                            notificationManager.updateEvents(updates);    
                            
                }

                @Override
                public void onFailure(Throwable e) {
                    LOGGER.warn("Toll route lookup failed", e);
                    callback.processed(false);
                }
            });
        } else {
            callback.processed(false);
        }
    }
}