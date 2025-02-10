package org.traccar.handler.events;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;
import org.traccar.session.state.TollRouteProcessor;
import org.traccar.session.state.TollRouteState;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

public class TollEventHandler extends BaseEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TollEventHandler.class);


    private final CacheManager cacheManager;
    private final Storage storage;

    private final long minimalDuration;

    @Inject
    public TollEventHandler(Config config, CacheManager cacheManager, Storage storage) {
        this.cacheManager = cacheManager;
        this.storage = storage;
        minimalDuration = config.getLong(Keys.EVENT_OVERSPEED_MINIMAL_DURATION) * 1000;

    }

    @Override
    public void onPosition(Position position, Callback callback) {
        long deviceId = position.getDeviceId();

        Device device = cacheManager.getObject(Device.class, position.getDeviceId());
        if (device == null) {
            return;
        }
        if (!PositionUtil.isLatest(cacheManager, position) || !position.getValid()) {
            return;
        }
        String positionTollRef = position.getString(Position.KEY_TOLL_REF);

        Boolean positionIsToll = position.getBoolean(Position.KEY_TOLL);
        String positionTollName = position.getString(Position.KEY_TOLL_NAME);

        TollRouteState state = TollRouteState.fromDevice(device);
        TollRouteProcessor.updateState(state, position, positionIsToll, positionTollRef, positionTollName,
                minimalDuration);
        if (state != null && state.isChanged()) {
            state.toDevice(device);
            try {
                storage.updateObject(device, new Request(
                        new Columns.Include("tollStartDistance", "tollrouteTime", "attributes"),
                        new Condition.Equals("id", device.getId())));
            } catch (StorageException e) {
                LOGGER.warn("Update device Toll error", e);
            }
        }
        if (state.getEvent() != null) {
            callback.eventDetected(state.getEvent());
        }



    }
}
