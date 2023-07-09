package org.traccar.database;

import org.junit.jupiter.api.Test;
import org.traccar.model.Device;
import org.traccar.model.Group;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GroupTreeTest {

    private static Group createGroup(long id, String name, long parent) {
        Group group = new Group();
        group.setId(id);
        group.setName(name);
        group.setGroupId(parent);
        return group;
    }

    private static Device createDevice(long id, String name, long parent) {
        Device device = new Device();
        device.setId(id);
        device.setName(name);
        device.setGroupId(parent);
        return device;
    }

    @Test
    public void testGetDescendants() {
        Collection<Group> groups = new ArrayList<>();
        groups.add(createGroup(1, "First", 0));
        groups.add(createGroup(2, "Second", 1));
        groups.add(createGroup(3, "Third", 2));
        groups.add(createGroup(4, "Fourth", 2));
        groups.add(createGroup(5, "Fifth", 4));

        Collection<Device> devices = new ArrayList<>();
        devices.add(createDevice(1, "One", 3));
        devices.add(createDevice(2, "Two", 5));
        devices.add(createDevice(3, "One", 5));

        GroupTree groupTree = new GroupTree(groups, devices);

        assertEquals(4, groupTree.getGroups(1).size());
        assertEquals(3, groupTree.getGroups(2).size());
        assertEquals(0, groupTree.getGroups(3).size());
        assertEquals(1, groupTree.getGroups(4).size());

        assertEquals(3, groupTree.getDevices(1).size());
        assertEquals(1, groupTree.getDevices(3).size());
        assertEquals(2, groupTree.getDevices(4).size());
    }

}
