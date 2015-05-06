package org.traccar.model;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class MiscFormatterTest {

    @Test
    public void testToString() throws Exception {

        Position position = new Position();
        position.set("a", "1");
        position.set("b", "2");
        position.set("a", "3");
        position.set("c", 3.555);
        
        assertEquals(position.getOther(), "<info><a>3</a><b>2</b><c>3.56</c></info>");
        
    }

}
