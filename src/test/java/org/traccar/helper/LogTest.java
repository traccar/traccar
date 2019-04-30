package org.traccar.helper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LogTest {
    
    @Test
    public void testExceptionStack() {
        assertEquals(
                "test - Exception (LogTest:11 < ...)",
                Log.exceptionStack(new Exception("test")));
    }

    @Test
    public void testExceptionStackRootCause() {
        assertEquals(
                "root - Exception (LogTest:18 < ...)",
                Log.exceptionStack(new Exception("test", new Exception("root"))));
    }

}
