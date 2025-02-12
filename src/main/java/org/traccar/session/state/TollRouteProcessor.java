package org.traccar.session.state;

import org.traccar.model.Event;
import org.traccar.model.Position;

public final class TollRouteProcessor {
    public static final String ATTRIBUTE_TOLL_DIST = "tollDistance";


    private TollRouteProcessor() {
    }

    public static void updateState(
            TollRouteState state, Position position,
            Boolean toll, String tollRef, String tollName, long minimalDuration) {

        if (tollRef != null) {
            state.setTollRef(tollRef);
        }
        if (tollName != null) {
            state.setTollName(tollName);
        }

        state.setEvent(null);



        double currentTotalDist =  position.getDouble(Position.KEY_TOTAL_DISTANCE);
        double oldTollDist = state.getTollStartDistance();
        if (oldTollDist > 0) {

            double currentTollDist = currentTotalDist - oldTollDist;
            if (currentTollDist < 0) { // if current distance traveled on toll is less invalid
                state.setTollStartDistance(0);
                checkEvent(state, position, currentTollDist, minimalDuration);
                state.setTollrouteTime(null);
            }
            if (toll) {
                checkEvent(state, position, currentTollDist, minimalDuration);
            } else {
                state.setTollStartDistance(0);
//                state.setTollrouteTime(position.getFixTime());
                checkEvent(state, position, currentTollDist, minimalDuration);
                state.setTollrouteTime(null);
            }
        } else if (position != null) {
            if (toll) {
                state.setTollStartDistance(currentTotalDist);
                state.setTollrouteTime(position.getFixTime());
                checkEvent(state, position, 0, minimalDuration);
            }
        }
    }

    private static void checkEvent(TollRouteState state, Position position, double tollDist, long minimalDuration) {
        //TODO: add logic to delay first event till name or ref is available
        if (state.getTollrouteTime() != null) {
            long oldTime = state.getTollrouteTime().getTime();
            long newTime = position.getFixTime().getTime();
            double tollStart = state.getTollStartDistance();
            if (newTime - oldTime >= minimalDuration) {
                Event event = null;
                if (tollStart == 0 && tollDist > 0) {
                    event = new Event(Event.TYPE_DEVICE_TOLLROUTE_EXIT, position);
                    event.set(ATTRIBUTE_TOLL_DIST, tollDist);
                } else if (tollStart > 0 && tollDist == 0) {
                    event = new Event(Event.TYPE_DEVICE_TOLLROUTE_ENTER, position);
                }
                if (event != null) {
                    event.set(Position.KEY_TOLL_NAME, state.getTollName());
                    if (state.getTollName() == null && state.getTollRef() != null) {
                        event.set(Position.KEY_TOLL_NAME, state.getTollRef());
                    }
                    if (state.getTollName() == null && state.getTollRef() == null) {
                        event.set(Position.KEY_TOLL_NAME, "");
                    }
                    event.set(Position.KEY_TOLL_REF, state.getTollRef());
                    state.setTollrouteTime(null);
                    state.setEvent(event);
                }
             }
        }
    }

}
