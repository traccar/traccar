package org.traccar.session.state;

import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.Date;
import java.util.Map;

public class TollRouteState {


    public static final String STATUS_ENTERED = "entered";
    public static final String STATUS_EXITED = "exited";
    public static final String STATUS_ON_TOLL = "toll";
    public static final String STATUS_OFF_TOLL = "noToll";


    public static TollRouteState fromDevice(Device device) {
        TollRouteState state = new TollRouteState();
        if (device.hasAttribute(Position.KEY_TOLL_NAME)) {
            state.tollName = device.getString(Position.KEY_TOLL_NAME);
        }
        if (device.hasAttribute(Position.KEY_TOLL_REF)) {
            state.tollRef = device.getString(Position.KEY_TOLL_REF);
        }
        state.tollStartDistance = device.getTollStartDistance();
        state.tollrouteTime = device.getTollrouteTime();
        return state;
    }

    public void toDevice(Device device) {
        if (tollName != null) {
            device.set(Position.KEY_TOLL_NAME, tollName);
        }
        if (tollRef != null) {
            device.set(Position.KEY_TOLL_REF, tollRef);
        }

        if (event != null && event.getType().equals(Event.TYPE_DEVICE_TOLLROUTE_EXIT)) {
            Map<String, Object> deviceAttributes = device.getAttributes();
            deviceAttributes.remove(Position.KEY_TOLL_REF);
            deviceAttributes.remove(Position.KEY_TOLL_NAME);
            device.setAttributes(deviceAttributes);
        }

        device.setTollStartDistance(tollStartDistance);
        device.setTollrouteTime(tollrouteTime);

    }

    private boolean changed;

    public boolean isChanged() {
        return changed;
    }

    private double tollStartDistance;

    public double getTollStartDistance() {
        return tollStartDistance;
    }

    public void setTollStartDistance(double tollStartDistance) {
        changed = true;
        this.tollStartDistance = tollStartDistance;
    }

    private Date tollrouteTime;

    public Date getTollrouteTime() {
        return tollrouteTime;
    }

    public void setTollrouteTime(Date tollrouteTime) {
        changed = true;
        this.tollrouteTime = tollrouteTime;
    }

    private Event event;

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    private String tollRef;

    public String getTollRef() {
        return tollRef;
    }

    public void setTollRef(String tollRef) {
        if (this.tollRef == null || !tollRef.equals(this.tollRef)) {
            changed = true;
        }
        this.tollRef = tollRef;
    }

    private String tollName;

    public String getTollName() {
        return tollName;
    }

    public void setTollName(String tollName) {
        changed = true;
        this.tollName = tollName;
    }




}
