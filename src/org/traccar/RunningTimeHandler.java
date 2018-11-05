package org.traccar;

import org.traccar.model.Device;
import org.traccar.model.Position;

public class RunningTimeHandler extends BaseDataHandler{

    @Override
    protected Position handlePosition(Position position) {
        Device device = Context.getIdentityManager().getById(position.getDeviceId());

        if (device == null || !Context.getIdentityManager().isLatestPosition(position)) {
            return position;
        }

        if (position.getAttributes().containsKey(Position.KEY_IGNITION)) {
            boolean ignition = position.getBoolean(Position.KEY_IGNITION);

            Position lastPosition = Context.getIdentityManager().getLastPosition(position.getDeviceId());

            if (lastPosition != null
                    && lastPosition.getAttributes().containsKey(Position.KEY_IGNITION)
                    && position.getDeviceTime().compareTo((lastPosition.getDeviceTime())) >= 0) {

                boolean oldIgnition = lastPosition.getBoolean(Position.KEY_IGNITION);

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
        return position;
    }

    private void initializeHourMeter(final Position position, final Position lastPosition) {
        long totalHours = lastPosition.getLong(Position.KEY_TOTAL_IGN_ON_MILLIS);
        position.set(Position.KEY_IGN_ON_MILLIS, 0L);
        position.set(Position.KEY_TOTAL_IGN_ON_MILLIS, totalHours);
    }

    private void continueRunningHourMeter(final Position position, final Position lastPosition) {

        long millisIgnOn = position.getDeviceTime().getTime() - lastPosition.getDeviceTime().getTime();
        long totalMillisIgnOn = lastPosition.getLong(Position.KEY_TOTAL_IGN_ON_MILLIS) + millisIgnOn;

        position.set(Position.KEY_IGN_ON_MILLIS, millisIgnOn);
        position.set(Position.KEY_TOTAL_IGN_ON_MILLIS, totalMillisIgnOn);
    }
}
