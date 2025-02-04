package org.traccar.session.state;

import org.traccar.model.Event;
import org.traccar.model.Position;

public final class TollRouteProcessor {
    public static final String ATTRIBUTE_SPEED = "speed";

    private TollRouteProcessor() {
    }

    public static void updateState(
            TollRouteState state, Position position,
            Boolean toll, String tollRef, String tollName, long minimalDuration) {

        state.setEvent(null);

        boolean oldState = state.getTollrouteState();
        if (oldState) {
            if (toll) {
                checkEvent(state, position, tollRef, tollName, minimalDuration);
            } else {
                state.setTollrouteState(false);
                state.setTollrouteTime(null);

            }
        } else if (position != null ) {
            state.setTollrouteState(true);
            state.setTollrouteTime(position.getFixTime());
            checkEvent(state, position, tollRef, tollName, minimalDuration);
        }
    }

    private static void checkEvent(TollRouteState state, Position position, String tollRef, String tollName, long minimalDuration) {
        if (state.getTollrouteTime() != null) {
            long oldTime = state.getTollrouteTime().getTime();
            long newTime = position.getFixTime().getTime();
            if (newTime - oldTime >= minimalDuration) {

                Event event = new Event(Event.TYPE_DEVICE_TOLLROUTE, position);
                event.set(ATTRIBUTE_SPEED, position.getSpeed());
                event.set(Position.KEY_TOLL_NAME, tollName);
                event.set(Position.KEY_TOLL_REF, tollRef);

                state.setTollrouteTime(null);
                state.setEvent(event);

            }
        }
    }
}
