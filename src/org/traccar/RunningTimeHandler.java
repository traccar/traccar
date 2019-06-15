package org.traccar;

import com.google.common.collect.ImmutableSet;
import org.eclipse.jetty.util.StringUtil;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.processing.peripheralsensorprocessors.fuelsensorprocessors.DeviceConsumptionInfo;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.traccar.model.Position.KEY_IGNITION;
import static org.traccar.model.Position.KEY_IGN_ON_MILLIS;
import static org.traccar.model.Position.KEY_TOTAL_IGN_ON_MILLIS;

public class RunningTimeHandler extends BaseDataHandler {

    private static long DATA_LOSS_FOR_IGNITION_MILLIS;

    static {
        DATA_LOSS_FOR_IGNITION_MILLIS =
                Context.getConfig().getLong("processing.peripheralSensorData.ignitionDataLossThresholdSeconds") * 1000L;
    }

    private Set<String> consumptionTypesWithoutIgnition = ImmutableSet.of("enginelesshourly", "noconsumption");
    private Map<Long, Position> latestPositionByDeviceId = new ConcurrentHashMap();

    @Override
    protected Position handlePosition(Position position) {

        Log.info(String.format("Calculating run time for: %d", position.getDeviceId()));

        long deviceId = position.getDeviceId();
        Device device = Context.getIdentityManager().getById(deviceId);

        if (device == null) {
            return position;
        }

        DeviceConsumptionInfo consumptionInfo = Context.getDeviceManager().getDeviceConsumptionInfo(deviceId);
        String consumptionType = consumptionInfo.getDeviceConsumptionType().toLowerCase();

        if (StringUtil.isNotBlank(consumptionType) && consumptionTypesWithoutIgnition.contains(consumptionType)) {
            position.set(KEY_IGN_ON_MILLIS, 0L);
            position.set(KEY_TOTAL_IGN_ON_MILLIS, 0);
            return position;
        }

        Position lastPosition = Context.getIdentityManager().getLastPosition(position.getDeviceId());

        if (!Context.getIdentityManager().isLatestPosition(position)) {
            initializeHourMeter(position);
            return position;
        }

        if (lastPosition == null) {
            initializeHourMeter(position);
            return position;
        }

        // We have a last position, check if data was lost in between since can't say if ignition was on or not then.
        if (position.getDeviceTime().getTime() - lastPosition.getDeviceTime().getTime() >= DATA_LOSS_FOR_IGNITION_MILLIS) {
            initializeHourMeter(position, lastPosition);
            return position;
        }

        if (!position.getAttributes().containsKey(KEY_IGNITION) &&
                !lastPosition.getAttributes().containsKey(KEY_IGN_ON_MILLIS)) {
            initializeHourMeter(position);
            return position;
        }

        if (!position.getAttributes().containsKey(KEY_IGNITION) &&
                lastPosition.getAttributes().containsKey(KEY_IGN_ON_MILLIS)) {

            carryValuesForward(position, lastPosition);
            return position;
        }

        if (position.getAttributes().containsKey(KEY_IGNITION) &&
                !lastPosition.getAttributes().containsKey(KEY_IGN_ON_MILLIS)) {
            initializeHourMeter(position);
            return position;
        }


        if (position.getAttributes().containsKey(KEY_IGNITION)
                && lastPosition.getAttributes().containsKey(KEY_IGN_ON_MILLIS)) {

            boolean ignition = position.getBoolean(KEY_IGNITION);



            if (lastPosition.getAttributes().containsKey(KEY_IGNITION)
                    && position.getDeviceTime().compareTo(lastPosition.getDeviceTime()) >= 0) {

                boolean oldIgnition = lastPosition.getBoolean(KEY_IGNITION);

                if (ignition && !oldIgnition) {
                    // Start hour meter
                    initializeHourMeter(position, lastPosition);
                } else if (!ignition && oldIgnition) {
                    // Continue hour meter, last increment
                    continueRunningHourMeter(position, lastPosition);
                } else if (ignition && oldIgnition) { // Has remained on.
                    // Keep the hour meter running
                    continueRunningHourMeter(position, lastPosition);
                } else if (!ignition && !oldIgnition) {

                    // Carry 0s over from last position.
                    initializeHourMeter(position, lastPosition);
                }
            }
        }
        Log.info(String.format("Done calculating run time for: %d", position.getDeviceId()));
        return position;
    }

    private void initializeHourMeter(final Position position) {

        if (!position.getAttributes().containsKey(KEY_IGNITION)) {
            position.set(KEY_IGNITION, false);
        }

        position.set(KEY_IGN_ON_MILLIS, 0L);
        position.set(KEY_TOTAL_IGN_ON_MILLIS, 0);
    }

    private void initializeHourMeter(final Position position, final Position lastPosition) {
        long totalHours = lastPosition.getLong(KEY_TOTAL_IGN_ON_MILLIS);
        position.set(KEY_IGN_ON_MILLIS, 0L);
        position.set(KEY_TOTAL_IGN_ON_MILLIS, totalHours);
    }

    private void continueRunningHourMeter(final Position position, final Position lastPosition) {

        long millisIgnOn = position.getDeviceTime().getTime() - lastPosition.getDeviceTime().getTime();
        long totalMillisIgnOn = lastPosition.getLong(KEY_TOTAL_IGN_ON_MILLIS) + millisIgnOn;

        position.set(KEY_IGN_ON_MILLIS, millisIgnOn);
        position.set(KEY_TOTAL_IGN_ON_MILLIS, totalMillisIgnOn);
    }

    private void carryValuesForward(final Position position, final Position lastPosition) {
        if (!position.getAttributes().containsKey(KEY_IGNITION)) {
            position.set(KEY_IGNITION, lastPosition.getBoolean(KEY_IGNITION));
        }

        position.set(KEY_IGN_ON_MILLIS, lastPosition.getLong(KEY_IGN_ON_MILLIS));
        position.set(KEY_TOTAL_IGN_ON_MILLIS, lastPosition.getLong(KEY_TOTAL_IGN_ON_MILLIS));
    }

    private void getLatestPositionBefore(Position lastPosition, Position position) {
        // NOTE: This is a no-op method only to preserve the calls we were making to retrieve the lastPositionBefore
        // These _may_ have been causing extra db strain, so we're not doing this for now. Ideally this could happen
        // async, since we use the totalIgnition time in the reports.
        try {
            Collection<Position> lastPositionsBefore = Context.getDataManager().getLastPositionBefore(position);
            if (lastPositionsBefore.size() > 0) {
                lastPosition = lastPositionsBefore.stream().findFirst().get();
            }
        } catch (SQLException e) {
            Log.debug(String.format("Exception while getting last position before deviceId: %d, deviceTime: %d",
                                    position.getDeviceId(), position.getDeviceTime().getTime()));
            e.printStackTrace();
            lastPosition = null;
        }
    }
}
