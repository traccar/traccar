package org.traccar.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AttributeMapTest {

    @Test
    public void testEmptyMap() {
        AttributeMap attributes = new AttributeMap();
        assertEquals(0, attributes.size());
        assertTrue(attributes.isEmpty());
        assertNull(attributes.get("missing"));
        assertFalse(attributes.containsKey("missing"));
    }

    @Test
    public void testPutAndGet() {
        AttributeMap attributes = new AttributeMap();
        assertNull(attributes.put("battery", 12.5));
        assertNull(attributes.put("rssi", 88));
        assertEquals(12.5, attributes.get("battery"));
        assertEquals(88, attributes.get("rssi"));
        assertEquals(2, attributes.size());
    }

    @Test
    public void testPutReturnsPreviousValue() {
        AttributeMap attributes = new AttributeMap();
        attributes.put("key", "first");
        assertEquals("first", attributes.put("key", "second"));
        assertEquals("second", attributes.get("key"));
        assertEquals(1, attributes.size());
    }

    @Test
    public void testOverwritePreservesPosition() {
        AttributeMap attributes = new AttributeMap();
        attributes.put("a", 1);
        attributes.put("b", 2);
        attributes.put("c", 3);
        attributes.put("a", 99);
        assertEquals(List.of("a", "b", "c"), new ArrayList<>(attributes.keySet()));
        assertEquals(99, attributes.get("a"));
    }

    @Test
    public void testInsertionOrder() {
        AttributeMap attributes = new AttributeMap();
        attributes.put("z", 1);
        attributes.put("a", 2);
        attributes.put("m", 3);
        assertEquals(List.of("z", "a", "m"), new ArrayList<>(attributes.keySet()));
    }

    @Test
    public void testRemove() {
        AttributeMap attributes = new AttributeMap();
        attributes.put("a", 1);
        attributes.put("b", 2);
        attributes.put("c", 3);
        assertEquals(2, attributes.remove("b"));
        assertEquals(2, attributes.size());
        assertFalse(attributes.containsKey("b"));
        assertEquals(List.of("a", "c"), new ArrayList<>(attributes.keySet()));
    }

    @Test
    public void testRemoveMissingKey() {
        AttributeMap attributes = new AttributeMap();
        attributes.put("a", 1);
        assertNull(attributes.remove("missing"));
        assertEquals(1, attributes.size());
    }

    @Test
    public void testRemoveLastEntry() {
        AttributeMap attributes = new AttributeMap();
        attributes.put("a", 1);
        attributes.put("b", 2);
        attributes.remove("b");
        attributes.put("c", 3);
        assertEquals(List.of("a", "c"), new ArrayList<>(attributes.keySet()));
    }

    @Test
    public void testClear() {
        AttributeMap attributes = new AttributeMap();
        attributes.put("a", 1);
        attributes.put("b", 2);
        attributes.clear();
        assertEquals(0, attributes.size());
        assertTrue(attributes.isEmpty());
        assertNull(attributes.get("a"));
    }

    @Test
    public void testGrowthBeyondInitialCapacity() {
        AttributeMap attributes = new AttributeMap();
        int entryCount = 50;
        for (int index = 0; index < entryCount; index++) {
            attributes.put("key" + index, index);
        }
        assertEquals(entryCount, attributes.size());
        for (int index = 0; index < entryCount; index++) {
            assertEquals(index, attributes.get("key" + index));
        }
    }

    @Test
    public void testEqualsAcrossMapTypes() {
        AttributeMap attributes = new AttributeMap();
        attributes.put("a", 1);
        attributes.put("b", "two");

        Map<String, Object> reference = new LinkedHashMap<>();
        reference.put("a", 1);
        reference.put("b", "two");

        assertEquals(attributes, reference);
        assertEquals(reference, attributes);
        assertEquals(reference.hashCode(), attributes.hashCode());
    }

    @Test
    public void testCopyConstructor() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        AttributeMap copy = new AttributeMap(source);
        assertEquals(source, copy);
        assertEquals(List.of("a", "b"), new ArrayList<>(copy.keySet()));
    }

    @Test
    public void testCopyConstructorWithNull() {
        AttributeMap attributes = new AttributeMap(null);
        assertTrue(attributes.isEmpty());
    }

    @Test
    public void testIteratorOrder() {
        AttributeMap attributes = new AttributeMap();
        attributes.put("first", 1);
        attributes.put("second", 2);
        attributes.put("third", 3);

        Iterator<Map.Entry<String, Object>> iterator = attributes.entrySet().iterator();
        assertEquals("first", iterator.next().getKey());
        assertEquals("second", iterator.next().getKey());
        assertEquals("third", iterator.next().getKey());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIteratorRemove() {
        AttributeMap attributes = new AttributeMap();
        attributes.put("a", 1);
        attributes.put("b", 2);
        attributes.put("c", 3);

        Iterator<Map.Entry<String, Object>> iterator = attributes.entrySet().iterator();
        iterator.next();
        iterator.next();
        iterator.remove();
        assertEquals(List.of("a", "c"), new ArrayList<>(attributes.keySet()));
    }

    @Test
    public void testEntrySetValueWritesThrough() {
        AttributeMap attributes = new AttributeMap();
        attributes.put("a", 1);
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            entry.setValue(99);
        }
        assertEquals(99, attributes.get("a"));
    }

    @Test
    public void testJacksonRoundtripPreservesType() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Device device = new Device();
        device.setUniqueId("abc");
        device.set("battery", 12.5);
        device.set("rssi", 88);
        device.set("ignition", true);

        String json = mapper.writeValueAsString(device);
        Device parsed = mapper.readValue(json, Device.class);

        assertSame(AttributeMap.class, parsed.getAttributes().getClass());
        assertEquals(device.getAttributes(), parsed.getAttributes());
        assertEquals(
                new ArrayList<>(device.getAttributes().keySet()),
                new ArrayList<>(parsed.getAttributes().keySet()));
    }

    @Test
    public void testSetAttributesAcceptsNull() {
        Device device = new Device();
        device.set("battery", 12.5);
        device.setAttributes(null);
        assertTrue(device.getAttributes().isEmpty());
    }

}
