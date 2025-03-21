package org.traccar.session.state;

import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.Date;

public final class TollRouteProcessor {

    public static final String ATTRIBUTE_TOLL_DIST = "tollDistance";

    private TollRouteProcessor() {
    }

    public static void updateState(
            TollRouteState state, Position position,
            String tollRef, String tollName, int  minimalDuration) {

        state.setEvent(null);

        double currentTotalDist =  position.getDouble(Position.KEY_TOTAL_DISTANCE);
        double startTollDist = state.getTollStartDistance();
        Boolean isOnToll = state.isOnToll(minimalDuration);
        if (isOnToll != null) {
            if (isOnToll) {
                if (startTollDist == 0) {   //entered toll
                    stateStartToll(state, currentTotalDist, position.getFixTime(), tollRef, tollName);
                    checkEvent(state, position, 0, currentTotalDist);
                } else if (startTollDist > 0) { // set names for tolls
                    if (state.getTollRef() == null && tollRef != null) {
                        state.setTollRef(tollRef);
                    }
                    if (state.getTollName() == null && tollName != null) {
                        state.setTollName(tollName);
                    }
                }
            } else if (startTollDist > 0) { // exited toll
                double currentTollDist = currentTotalDist - startTollDist;
                if (state.getTollExitDistance() == -1) { // good exit (enter notif was sent)
                    state.setTollExitDistance(currentTotalDist);
                    state.setTollrouteTime(position.getFixTime());

                    checkEvent(state, position, currentTollDist, 0);
                    state.setTollStartDistance(0);
                    state.setTollrouteTime(null);
                } else if (state.getTollExitDistance() == 0) { // bad exit (no enter event)
                    state.setTollStartDistance(0);
                    state.setTollrouteTime(null);
                }
            }
        }
    }

    private static void checkEvent(TollRouteState state, Position position, double tollDist,
                                      double tollStart) {
        if (state.getTollrouteTime() != null) {
            Event event = null;
            if (tollStart > 0) {
                event = new Event(Event.TYPE_DEVICE_TOLLROUTE_ENTER, position);
                state.setTollExitDistance(-1);
            } else if (tollStart == 0) {
                 event = new Event(Event.TYPE_DEVICE_TOLLROUTE_EXIT, position);
                event.set(ATTRIBUTE_TOLL_DIST, tollDist);
            }

            if (event != null) {
                event.set(Position.KEY_TOLL_NAME, state.getTollName());
                if (state.getTollName() == null && state.getTollRef() != null) {
                    event.set(Position.KEY_TOLL_NAME, state.getTollRef());
                }
                if (state.getTollName() == null && state.getTollRef() == null) {
                    event.set(Position.KEY_TOLL_NAME, " ");
                }
                event.set(Position.KEY_TOLL_REF, state.getTollRef());
                state.setTollrouteTime(null);
                state.setEvent(event);
                return;
            }
        }
    }

    private static void stateStartToll(TollRouteState state, double tollStartDistance, Date startTime,
                                       String tollRef, String tollName) {
        state.setTollStartDistance(tollStartDistance);
        state.setTollExitDistance(0);
        state.setTollrouteTime(startTime);
        state.setTollRef(tollRef);
        state.setTollName(tollName);

    }

}
