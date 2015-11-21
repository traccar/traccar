package org.traccar.web;

import org.junit.Assert;
import org.junit.Test;

public class AsyncServletTest {

    @Test
    public void testDebugDisabled() {
        Assert.assertFalse("debugging enabled", AsyncServlet.AsyncSession.DEBUG_ASYNC);
    }

}
