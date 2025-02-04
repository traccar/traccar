package org.traccar.tollroute;

public class TollData {
    private final Boolean toll;
    private final String ref;
    private final String name;

    public TollData(Boolean toll, String ref, String name) {
        this.toll = toll;
        this.ref = ref;
        this.name = name;
    }

    public Boolean getToll() { return toll; }
    public String getRef() { return ref; }
    public String getName() { return name; }
    
}
