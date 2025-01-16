package org.traccar.tollroute;

public class TollData {
        private final String toll;
    private final String ref;
    private final String name;

    public TollData(String toll, String ref, String name) {
        this.toll = toll;
        this.ref = ref;
        this.name = name;
    }

    public String getToll() { return toll; }
    public String getRef() { return ref; }
    public String getName() { return name; }
    
}
