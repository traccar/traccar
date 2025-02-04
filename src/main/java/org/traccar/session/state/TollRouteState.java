package org.traccar.session.state;

import org.traccar.model.Device;
import org.traccar.model.Event;

import java.util.Date;

public class TollRouteState {

    public static TollRouteState fromDevice(Device device) {
        TollRouteState state = new TollRouteState();
        state.tollRouteState = device.getTollrouteState();
        state.tollrouteTime = device.getTollrouteTime();
//        state.tollrouteGeofenceId = device.getTollrouteGeofenceId();
        return state;
    }

    public void toDevice(Device device) {
        device.setTollrouteState(tollRouteState);
//        device.setTollrouteTime(tollrouteTime);
//        device.setTollrouteGeofenceId(tollrouteGeofenceId);
    }

    private boolean changed;

    public boolean isChanged() {
        return changed;
    }

    private boolean tollRouteState;

    public boolean getTollrouteState() {
        return tollRouteState;
    }

    public void setTollrouteState(boolean tollRouteState) {
        this.tollRouteState = tollRouteState;
        changed = true;
    }

    private Date tollrouteTime;

    public Date getTollrouteTime() {
        return tollrouteTime;
    }

    public void setTollrouteTime(Date tollrouteTime) {
        this.tollrouteTime = tollrouteTime;
        changed = true;
    }

    private Event event;

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

}
