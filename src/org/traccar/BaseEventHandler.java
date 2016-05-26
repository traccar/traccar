package org.traccar;

import org.traccar.model.Event;
import org.traccar.model.Position;

public abstract class BaseEventHandler extends BaseDataHandler {

    private boolean isLastPosition = false;

    public boolean isLastPosition() {
        return isLastPosition;
    }

    @Override
    protected Position handlePosition(Position position) {

        Position lastPosition = Context.getConnectionManager().getLastPosition(position.getDeviceId());
        if (lastPosition == null || position.getFixTime().compareTo(lastPosition.getFixTime()) >= 0) {
            isLastPosition = true;
        }

        Event event = analizePosition(position);
        if (event != null) {
            Context.getConnectionManager().updateEvent(event);
        }
        return position;
    }

    protected abstract Event analizePosition(Position position);

}
