package org.traccar.helper;

import org.junit.Assert;
import org.junit.Test;

public class LogTest {
    
    @Test
    public void testLog() {
        Assert.assertEquals("test - Exception (LogTest:10 < ...)", Log.exceptionStack(new Exception("test")));
    }

}
