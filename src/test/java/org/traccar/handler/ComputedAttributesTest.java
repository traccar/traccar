package org.traccar.handler;

import org.junit.Test;
import org.traccar.config.Config;
import org.traccar.model.Attribute;
import org.traccar.model.Position;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class ComputedAttributesTest {

    @Test
    public void testComputedAttributes() {

        ComputedAttributesHandler handler = new ComputedAttributesHandler(new Config(), null);

        Date date = new Date();
        Position position = new Position();
        position.setTime(date);
        position.setSpeed(42);
        position.setValid(false);
        position.set("adc1", 128);
        position.set("booleanFlag", true);
        position.set("adc2", 100);
        position.set("bitFlag", 7);
        position.set("event", 42);
        position.set("result", "success");
        Attribute attribute = new Attribute();

        attribute.setExpression("adc1");
        assertEquals(128, handler.computeAttribute(attribute, position));

        attribute.setExpression("!booleanFlag");
        assertEquals(false, handler.computeAttribute(attribute, position));

        attribute.setExpression("adc2 * 2 + 50");
        assertEquals(250, handler.computeAttribute(attribute, position));

        attribute.setExpression("(bitFlag & 4) != 0");
        assertEquals(true, handler.computeAttribute(attribute, position));

        attribute.setExpression("if (event == 42) \"lowBattery\"");
        assertEquals("lowBattery", handler.computeAttribute(attribute, position));

        attribute.setExpression("speed > 5 && valid");
        assertEquals(false, handler.computeAttribute(attribute, position));

        attribute.setExpression("fixTime");
        assertEquals(date, handler.computeAttribute(attribute, position));

        attribute.setExpression("math:pow(adc1, 2)");
        assertEquals(16384.0, handler.computeAttribute(attribute, position));

        // modification tests
        attribute.setExpression("adc1 = 256");
        handler.computeAttribute(attribute, position);
        assertEquals(128, position.getInteger("adc1"));

        attribute.setExpression("result = \"fail\"");
        handler.computeAttribute(attribute, position);
        assertEquals("success", position.getString("result"));

        attribute.setExpression("fixTime = \"2017-10-18 10:00:01\"");
        handler.computeAttribute(attribute, position);
        assertEquals(date, position.getFixTime());

    }

}
