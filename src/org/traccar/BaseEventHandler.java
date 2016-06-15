package org.traccar;

import java.util.Collection;

import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

public abstract class BaseEventHandler extends BaseDataHandler {

    private boolean isLastPosition = false;

    public boolean isLastPosition() {
        return isLastPosition;
    }

    private Device device;

    public Device getDevice() {
        return device;
    }

    @Override
    protected Position handlePosition(Position position) {

        device = Context.getDataManager().getDeviceById(position.getDeviceId());
        if (device != null) {
            long lastPositionId = device.getPositionId();
            if (position.getId() == lastPositionId) {
                isLastPosition = true;
            }
        }

        Collection<Event> events = analizePosition(position);
        if (events != null) {
            for (Event event : events) {
                Context.getNotificationManager().updateEvent(event, position);
            }
        }
        return position;
    }

    protected abstract Collection<Event> analizePosition(Position position);

}
