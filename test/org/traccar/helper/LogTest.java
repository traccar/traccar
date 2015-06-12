package org.traccar.helper;

import org.junit.Assert;
import org.junit.Test;

public class LogTest {
    
    @Test
    public void testLog() {
        Assert.assertEquals("test - Exception (LogTest.java:10)", Log.exception(new Exception("test")));
    }

}
