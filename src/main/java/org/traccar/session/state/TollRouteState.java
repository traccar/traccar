package org.traccar.session.state;

import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

import java.util.*;

public class TollRouteState {

    public void fromDevice(Device device) {
        if (device.hasAttribute(Position.KEY_TOLL_NAME)) {
            this.tollName = device.getString(Position.KEY_TOLL_NAME);
        }
        if (device.hasAttribute(Position.KEY_TOLL_REF)) {
            this.tollRef = device.getString(Position.KEY_TOLL_REF);
        }
        this.tollStartDistance = device.getTollStartDistance();
        this.tollExitDistance = device.getDouble(Position.KEY_TOLL_EXIT);
        this.tollrouteTime = device.getTollrouteTime();
    }

    public void toDevice(Device device) {
        if (tollName != null) {
            device.set(Position.KEY_TOLL_NAME, tollName);
        }
        if (tollRef != null) {
            device.set(Position.KEY_TOLL_REF, tollRef);
        }
        device.set(Position.KEY_TOLL_EXIT, tollExitDistance);


        if (event != null && event.getType().equals(Event.TYPE_DEVICE_TOLLROUTE_EXIT)) {
            Map<String, Object> deviceAttributes = device.getAttributes();
            deviceAttributes.remove(Position.KEY_TOLL_REF);
            deviceAttributes.remove(Position.KEY_TOLL_NAME);
            deviceAttributes.remove(Position.KEY_TOLL_EXIT);
            device.setAttributes(deviceAttributes);
        }

        device.setTollStartDistance(tollStartDistance);
        device.setTollrouteTime(tollrouteTime);

    }

    private boolean changed;

    public boolean isChanged() {
        return changed;
    }


    private List<Boolean> tollWindow;

    public void addOnToll(Boolean isToll) {
        if (this.tollWindow == null) {
            this.tollWindow = new ArrayList<Boolean>();
        }
        this.tollWindow.add(isToll);
        if (this.tollWindow.size() > 6) {
            this.tollWindow.remove(0);
        }
    }

    public Boolean isOnToll(int duration) {
        Set<Boolean> tollWindowSet = new HashSet<Boolean>(this.tollWindow);
        if (tollWindowSet.size() == 1) {
            if (this.tollWindow.size() == (int) duration) {
                return tollWindowSet.iterator().next();
            } else if (this.tollWindow.size() < 6 && tollWindowSet.contains(false)) {
                return false;
            }
        }
        return null;
    }



    private double tollStartDistance;

    public double getTollStartDistance() {
        return tollStartDistance;
    }

    public void setTollStartDistance(double tollStartDistance) {
        this.changed = true;
        this.tollStartDistance = tollStartDistance;
    }

    private double tollExitDistance;

    public double getTollExitDistance() {
        return tollExitDistance;
    }

    public void setTollExitDistance(double tollExitDistance) {
        this.changed = true;
        this.tollExitDistance = tollExitDistance;
    }

    private Date tollrouteTime;

    public Date getTollrouteTime() {
        return tollrouteTime;
    }

    public void setTollrouteTime(Date tollrouteTime) {
        this.changed = true;
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
        if (tollRef != null) {
            if (this.tollRef == null || !tollRef.equals(this.tollRef)) {
                this.changed = true;
                this.tollRef = tollRef;
            }
        }
    }

    private String tollName;

    public String getTollName() {
        return tollName;
    }

    public void setTollName(String tollName) {
        this.changed = true;
        this.tollName = tollName;
    }




}
