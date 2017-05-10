package org.traccar.processing;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.model.Attribute;
import org.traccar.model.Position;

public class ComputedAttributesTest {
    
    @Test
    public void testComputedAttributes() {
        Position position = new Position();
        ComputedAttributesHandler computedAttributesHandler = new ComputedAttributesHandler();
        position.set("adc1", 128);
        position.set("booleanFlag", true);
        position.set("adc2", 100);
        position.set("bitFlag", 7);
        position.set("event", 42);
        Attribute attribute = new Attribute();

        attribute.setExpression("position.getInteger(\"adc1\")");
        Assert.assertEquals(128, computedAttributesHandler.computeAttribute(attribute, position));

        attribute.setExpression("!position.getBoolean(\"booleanFlag\")");
        Assert.assertEquals(false, computedAttributesHandler.computeAttribute(attribute, position));

        attribute.setExpression("position.getInteger(\"adc2\") * 2 + 50");
        Assert.assertEquals(250, computedAttributesHandler.computeAttribute(attribute, position));

        attribute.setExpression("(position.getLong(\"bitFlag\") & 4) != 0");
        Assert.assertEquals(true, computedAttributesHandler.computeAttribute(attribute, position));

        attribute.setExpression("if (position.getLong(\"event\") == 42) \"lowBattery\"");
        Assert.assertEquals("lowBattery", computedAttributesHandler.computeAttribute(attribute, position));
    }

}
