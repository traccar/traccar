package org.traccar.session.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;

import static org.traccar.handler.events.TollEventHandler.LOGGER;

public class TollRouteState {
    private static final Logger LOGGER = LoggerFactory.getLogger(TollRouteState.class);


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
        this.id = device.getId();
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

    @JsonProperty
    private long id;
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    @JsonProperty
    private boolean changed;

    public boolean isChanged() {
        return changed;
    }
    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    @JsonProperty
    public List<Boolean> getTollWindow() {
        return tollWindow;
    }

    public void setTollWindow(List<Boolean> tollWindow) {
        this.tollWindow = tollWindow;
    }



    // private List<Boolean> tollWindow;
   @JsonProperty
   private List<Boolean> tollWindow = new ArrayList<>();

/*
    public void addOnToll(Boolean isToll, int duration) {
        if (this.tollWindow == null) {
            this.tollWindow = new ArrayList<Boolean>();
        }
        this.tollWindow.add(isToll);
        LOGGER.info("TollWindow added value: {}, current size: {}, values: {}", isToll, this.tollWindow.size(), this.tollWindow);
        if (this.tollWindow.size() > duration) {
            this.tollWindow.remove(0);
        }
    }
*/

    public void addOnToll(Boolean isToll, int duration) {
        if (isToll != null) {
            this.tollWindow.add(isToll);
            if (this.tollWindow.size() > duration) {
                this.tollWindow.remove(0);
            }
            LOGGER.info("TollWindow added value: {}, current size: {}, values: {}", isToll, this.tollWindow.size(), this.tollWindow);
        }
    }


    public Boolean isOnToll(int duration) {
        Set<Boolean> tollWindowSet = null;
        if (this.tollWindow != null) {
            tollWindowSet = new HashSet<>(this.tollWindow);
            LOGGER.info("TollWindow current size: {}, values: {}", this.tollWindow.size(), this.tollWindow);
        }
        if (tollWindowSet != null && tollWindowSet.size() == 1) {
            if (this.tollWindow.size() == (int) duration) {
                LOGGER.info("TollWindow reached required size {} with same value: {}", duration, tollWindowSet.iterator().next());
                return tollWindowSet.iterator().next();
            } else if (this.tollWindow.size() < duration && tollWindowSet.contains(false)) {
                LOGGER.info("TollWindow not yet at required size {}, but contains false", duration);
                return false;
            }
        }
        return null;
    }



    @JsonProperty
    private double tollStartDistance;

    public double getTollStartDistance() {
        return tollStartDistance;
    }

    public void setTollStartDistance(double tollStartDistance) {
        this.changed = true;
        this.tollStartDistance = tollStartDistance;
    }

    @JsonProperty
    private double tollExitDistance;

    public double getTollExitDistance() {
        return tollExitDistance;
    }

    public void setTollExitDistance(double tollExitDistance) {
        this.changed = true;
        this.tollExitDistance = tollExitDistance;
    }

    @JsonProperty
    private Date tollrouteTime;

    public Date getTollrouteTime() {
        return tollrouteTime;
    }

    public void setTollrouteTime(Date tollrouteTime) {
        this.changed = true;
        this.tollrouteTime = tollrouteTime;
    }

    @JsonIgnore
    private Event event;

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    @JsonProperty
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

    @JsonProperty
    private String tollName;

    public String getTollName() {
        return tollName;
    }
/*
    public void setTollName(String tollName) {
        this.changed = true;
        this.tollName = tollName;
    }
*/

    public void setTollName(String tollName) {
        if (tollName != null) {
            if (this.tollName == null || !tollName.equals(this.tollName)) {
                this.changed = true;
                this.tollName = tollName;
            }
        }
    }



}
