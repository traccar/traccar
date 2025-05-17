package org.traccar.model;

import org.traccar.storage.StorageName;

@StorageName("tc_organization")
public class Organization extends ExtendedModel{
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
