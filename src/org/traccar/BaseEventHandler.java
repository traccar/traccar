package org.traccar;

import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

public abstract class BaseEventHandler extends BaseDataHandler {

    private boolean isLastPosition = false;

    public boolean isLastPosition() {
        return isLastPosition;
    }

    @Override
    protected Position handlePosition(Position position) {

        Device device = Context.getDataManager().getDeviceById(position.getDeviceId());
        if (device != null) {
            long lastPositionId = device.getPositionId();
            if (position.getId() == lastPositionId) {
                isLastPosition = true;
            }
        }

        Event event = analizePosition(position);
        if (event != null) {
            Context.getConnectionManager().updateEvent(event);
        }
        return position;
    }

    protected abstract Event analizePosition(Position position);

}
