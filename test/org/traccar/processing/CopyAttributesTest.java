package org.traccar.processing;

import org.junit.Assert;
import org.junit.Test;
import org.traccar.model.Position;

public class CopyAttributesTest {

    @Test
    public void testCopyAttributes() {
        Position lastposition = new Position();
        lastposition.set("adc1", 1);
        lastposition.set("adc2", 1);
        lastposition.set("adc4", 1);

        Position position = new Position();
        position.set("adc2", 2);
        position.set("adc3", 2);

        String attributesString = "adc1,adc2";

        CopyAttributesHandler copyAttributesHandler = new CopyAttributesHandler();
        copyAttributesHandler.copyAttributes(attributesString, position, lastposition);

        Assert.assertEquals(1, position.getInteger("adc1"));
        Assert.assertEquals(2, position.getInteger("adc2"));
        Assert.assertEquals(2, position.getInteger("adc3"));
        Assert.assertFalse(position.getAttributes().containsKey("adc4"));
    }

}
