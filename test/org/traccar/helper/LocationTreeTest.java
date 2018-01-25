package org.traccar.helper;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LocationTreeTest {
    
    @Test
    public void testLocationTree() {

        List<LocationTree.Item> items = new ArrayList<>();
        items.add(new LocationTree.Item(1, 1, "a"));
        items.add(new LocationTree.Item(3, 2, "b"));
        items.add(new LocationTree.Item(1, 3, "c"));
        items.add(new LocationTree.Item(4, 3, "d"));

        LocationTree tree = new LocationTree(items);

        assertEquals("a", tree.findNearest(new LocationTree.Item(1f, 1f)).getData());
        assertEquals("d", tree.findNearest(new LocationTree.Item(10f, 10f)).getData());
        assertEquals("c", tree.findNearest(new LocationTree.Item(1f, 2.5f)).getData());
        assertEquals("a", tree.findNearest(new LocationTree.Item(1.5f, 1.5f)).getData());

    }

}
