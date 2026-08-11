package org.traccar.handler.events;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;

import java.util.Date;

public class IdleEventHandler extends BaseEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdleEventHandler.class);
    private static final long DEFAULT_IDLE_THRESHOLD = 20 * 60 * 1000; // 20 Min

    private final CacheManager cacheManager;
    private final Storage storage;
    private final long idleThreshold;
    private final boolean active; // True?

    @Inject
    public IdleEventHandler(Config config, CacheManager cacheManager, Storage storage) {
        this.cacheManager = cacheManager;
        this.storage = storage;
        long threshold = DEFAULT_IDLE_THRESHOLD;
        boolean tmpActive = false;

        try {
            tmpActive = config.getBoolean(Keys.PROCESSING_EVENT_IDLE);
            if (!tmpActive) {
                LOGGER.info("processing.event.idle key disabled (false)");
            }

            long minutes = config.getLong(Keys.EVENT_IDLE_MINIMUM_DURATION);
            if (minutes > 0) {
                threshold = minutes * 60 * 1000;
            } else {
                LOGGER.debug("Invalid or missing EVENT_IDLE_MINIMUM_DURATION, using default 30 minutes");
            }

        } catch (Exception e) {
            LOGGER.warn("IdleEventHandler: failed to read config keys, using defaults", e);
        }

        this.idleThreshold = threshold;
        this.active = tmpActive;
    }
    
    @Override
    public void onPosition(Position position, Callback callback) {
        // 📌 false ?
        if (!active) {
            LOGGER.info("İdle false  - devre dışı");
            return;
        }

        long deviceId = position.getDeviceId();
        Position last = cacheManager.getPosition(deviceId);

        boolean ignition = position.getBoolean(Position.KEY_IGNITION);
        boolean motion = position.getBoolean(Position.KEY_MOTION);
        double speed = position.getSpeed();
        Date now = new Date();

        if (!ignition || motion) {
            return; // İgnition off
        }
        
        // Last position
        boolean lastIgnition = false;
        boolean lastMotion = false;
        double lastSpeed = 0.0;
        if (last != null) {
            lastIgnition = last.getBoolean(Position.KEY_IGNITION);
            lastMotion = last.getBoolean(Position.KEY_MOTION);
            lastSpeed = last.getSpeed();
        } else {
            return;
        }


        
        boolean resetIdle = false;
        if (!lastIgnition && ignition) { // ignition off → on
            resetIdle = true;
        } else if (lastMotion && !motion) { //  moving > !moving
            resetIdle = true;
        } else if (lastSpeed > 1.0 && speed <= 1.0) { // speed 0
            resetIdle = true;
        }

        if (resetIdle) {
            position.getAttributes().put("idleStartTime", String.valueOf(now.getTime()));
            position.getAttributes().put("idleAlarmStatus", false);
            return;
        }

        // get idleStartTime 
        Date idleStart = null;
        if (last != null && last.getAttributes().containsKey("idleStartTime")) {
            Object v = last.getAttributes().get("idleStartTime");
            if (v instanceof Number) idleStart = new Date(((Number) v).longValue());
            else if (v instanceof String) idleStart = new Date(Long.parseLong((String) v));
        }

        if (idleStart == null) {
            position.getAttributes().put("idleStartTime", String.valueOf(now.getTime()));
            position.getAttributes().put("idleAlarmStatus", false);
            return;
        }

        // Alarm Status
        boolean alarmStatus = false;
        if (last != null && last.getAttributes().containsKey("idleAlarmStatus")) {
            Object a = last.getAttributes().get("idleAlarmStatus");
            if (a instanceof Boolean) alarmStatus = (Boolean) a;
            else if (a instanceof String) alarmStatus = Boolean.parseBoolean((String) a);
        }

        // idle threshold 
        long diff = now.getTime() - idleStart.getTime();
        if (!alarmStatus && diff >= idleThreshold) {
            Event event = new Event(Event.TYPE_ALARM, position);
            event.set(Position.KEY_ALARM, Position.ALARM_IDLE);
            callback.eventDetected(event);

            // Alarm Detected, Status Update
            position.getAttributes().put("idleAlarmStatus", true);
            position.getAttributes().put("idleStartTime", String.valueOf(now.getTime() + idleThreshold));
            //It is intended to prevent additional alarms from being affected when the device data is loaded in a mixed manner, and the time will be updated in the future time.
            try {
                position.setId(storage.addObject(position, new Request(new Columns.Exclude("id"))));
                cacheManager.updatePosition(position);
            } catch (StorageException e) {
                LOGGER.error("Failed to persist idle attributes for device {}", deviceId, e);
            }
        }
    }
}
