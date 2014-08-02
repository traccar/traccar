package org.traccar.model;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ExtendedInfoFormatterTest {

    @Test
    public void testToString() throws Exception {
        
        ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("test");
        extendedInfo.set("a", "1");
        extendedInfo.set("b", "2");
        extendedInfo.set("a", "3");
        
        assertEquals(extendedInfo.toString(), "<info><protocol>test</protocol><a>3</a><b>2</b></info>");
        
    }

}
