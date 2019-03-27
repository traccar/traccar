package org.traccar.helper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LogTest {
    
    @Test
    public void testLog() {
        assertEquals("test - Exception (LogTest:11 < ...)", Log.exceptionStack(new Exception("test")));
    }

}
