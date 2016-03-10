package org.traccar.database;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.model.Group;

import java.util.ArrayList;
import java.util.Collection;

public class GroupTreeTest {

    private static Group createGroup(long id, String name, long parent) {
        Group group = new Group();
        group.setId(id);
        group.setName(name);
        group.setGroupId(parent);
        return group;
    }
    
    @Test
    public void testGetDescendants() {
        Collection<Group> groups = new ArrayList<>();
        groups.add(createGroup(1, "First", 0));
        groups.add(createGroup(2, "Second", 1));
        groups.add(createGroup(3, "Third", 2));
        groups.add(createGroup(4, "Fourth", 2));
        groups.add(createGroup(5, "Fifth", 4));

        GroupTree groupTree = new GroupTree(groups);

        Assert.assertEquals(4, groupTree.getDescendants(1).size());
        Assert.assertEquals(3, groupTree.getDescendants(2).size());
        Assert.assertEquals(0, groupTree.getDescendants(3).size());
        Assert.assertEquals(1, groupTree.getDescendants(4).size());
    }

}
