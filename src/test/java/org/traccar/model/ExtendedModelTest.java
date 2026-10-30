package org.traccar.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.AbstractMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ExtendedModelTest {

    private final String testKey = "test";

    @Test
    public void testByteSetterIgnoresNull() {
        ExtendedModel extendedModel = new ExtendedModel();
        extendedModel.set(testKey, (Byte) null);

        assertFalse(extendedModel.hasAttribute(testKey));
    }

    @Test
    public void testShortSetterIgnoresNull() {
        ExtendedModel extendedModel = new ExtendedModel();
        extendedModel.set(testKey, (Short) null);

        assertFalse(extendedModel.hasAttribute(testKey));
    }

    @Test
    public void testFloatSetterIgnoresNull() {
        ExtendedModel extendedModel = new ExtendedModel();
        extendedModel.set(testKey, (Float) null);

        assertFalse(extendedModel.hasAttribute(testKey));
    }

    @Test
    public void testAddIgnoresNullObject() {
        ExtendedModel extendedModel = new ExtendedModel();
        Map<String, Object> attributes = extendedModel.getAttributes();
        extendedModel.add(null);

        assertEquals(attributes, extendedModel.getAttributes());
    }

    @Test
    public void testAddIgnoresNullValue() {
        Map.Entry<String, Object> entry = new AbstractMap.SimpleEntry<>(testKey, null);
        ExtendedModel extendedModel = new ExtendedModel();
        Map<String, Object> attributes = extendedModel.getAttributes();
        extendedModel.add(entry);

        assertEquals(attributes, extendedModel.getAttributes());
    }

    @Test
    public void testParseAsStringReturnsDefaultWhenNull() {
        ExtendedModel extendedModel = new ExtendedModel();
        String defaultValue = "Test default value";
        String result = extendedModel.getString(testKey, defaultValue);

        assertEquals(defaultValue, result);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-5.3, -1, -0.01, 0, 0.5, 11, 12.6})
    public void testParseDoubleWhenValueIsNumber(Number value) {
        ExtendedModel extendedModel = new ExtendedModel();
        extendedModel.add(new AbstractMap.SimpleEntry<>(testKey, value));
        Double result = extendedModel.getDouble(testKey, 0);

        assertEquals(value.doubleValue(), result);
    }

    @ParameterizedTest
    @ValueSource(strings= {"-5.3", "-1", "-0.01", "0", "0.5", "11", "12.6"})
    public void testParseDoubleWhenValueIsADoubleString(String value) {
        ExtendedModel extendedModel = new ExtendedModel();
        extendedModel.add(new AbstractMap.SimpleEntry<>(testKey, value));
        Double result = extendedModel.getDouble(testKey, 0);

        assertEquals(Double.parseDouble(value), result);
    }

    // it is unclear if the method should throw an exception or
    // return a default value
    @Test
    public void testParseDoubleWhenNotParsableAsDouble() {
        ExtendedModel extendedModel = new ExtendedModel();
        extendedModel.add(new AbstractMap.SimpleEntry<>(testKey, "not a double"));

        assertDoesNotThrow(() -> extendedModel.getDouble(testKey, 0));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testParseAsBooleanValueWhenValueIsBooleanObject(Boolean value) {
        ExtendedModel extendedModel = new ExtendedModel();
        extendedModel.add(new AbstractMap.SimpleEntry<>(testKey, (Boolean) value));
        Boolean result = extendedModel.getBoolean(testKey);

        assertEquals(value, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void testParseAsBooleanValueWhenValueIsBooleanString(String value) {
        ExtendedModel extendedModel = new ExtendedModel();
        extendedModel.add(new AbstractMap.SimpleEntry<>(testKey, value));
        Boolean result = extendedModel.getBoolean(testKey);

        assertEquals(Boolean.parseBoolean(value), result);
    }

    @ParameterizedTest
    @ValueSource(ints = {-124, -1, 0, 1, 125})
    public void testParseAsBooleanValueWhenValueIsInteger(Integer value) {
        ExtendedModel extendedModel = new ExtendedModel();
        extendedModel.add(new AbstractMap.SimpleEntry<>(testKey, value));
        Boolean result = extendedModel.getBoolean(testKey);

        assertEquals(Boolean.parseBoolean(String.valueOf(value)), result);
    }

    @Test
    public void testParseAsBooleanValueWhenValueIsAnObject() {
        ExtendedModel extendedModel = new ExtendedModel();
        Object obj = new Object();
        extendedModel.add(new AbstractMap.SimpleEntry<>(testKey, obj));
        Boolean result = extendedModel.getBoolean(testKey);

        assertEquals(Boolean.parseBoolean(obj.toString()), result);
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE})
    public void testParseAsLongWhenValuesAreNumbers(Number number) {
        ExtendedModel extendedModel = new ExtendedModel();
        extendedModel.add(new AbstractMap.SimpleEntry<>(testKey, number));
        Long result = extendedModel.getLong(testKey);

        assertEquals(number.longValue(), result);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-5.3, -1, -0.01, 0, 0.5, 11, 12.6})
    public void testParseAsLongWhenValuesAreDoubleNumbers(Number number) {
        ExtendedModel extendedModel = new ExtendedModel();
        extendedModel.add(new AbstractMap.SimpleEntry<>(testKey, number));
        Long result = extendedModel.getLong(testKey);

        assertEquals((long) number.doubleValue(), result);
    }

    @ParameterizedTest
    @ValueSource(strings= {"-1", "0", "11", "12"})
    public void testParseAsLongWhenValuesAreStringNumbers(String value) {
        ExtendedModel extendedModel = new ExtendedModel();
        extendedModel.add(new AbstractMap.SimpleEntry<>(testKey, value));
        Long result = extendedModel.getLong(testKey);

        assertEquals(Long.parseLong(value), result);
    }

    // it is unclear how unparsable objects should be treated
    @Test
    public void testParseAsLongWhenValueIsNotParsable() {
        ExtendedModel model = new ExtendedModel();
        Object obj = new Object();
        model.add(new AbstractMap.SimpleEntry<>(testKey, obj));

        assertDoesNotThrow(() -> model.getLong(testKey));
    }

    @ParameterizedTest
    @ValueSource(floats = {1.0f, 2.5f, 3.14f})
    public void testParseAsLongWhenFloatingPointValueIsProvided(Float value) {
        ExtendedModel model = new ExtendedModel();
        model.add(new AbstractMap.SimpleEntry<>(testKey, value));

        assertDoesNotThrow(() -> model.getLong(testKey));
    }

    // it is unclear how floating point strings should be parsed
    @ParameterizedTest
    @ValueSource(strings = {"1.0", "2.5", "3.14"})
    public void testParseAsLongWhenFloatingPointStringsProvided(String value) {
        ExtendedModel model = new ExtendedModel();
        model.add(new AbstractMap.SimpleEntry<>(testKey, value));

        assertDoesNotThrow(() -> model.getLong(testKey));
    }
}
