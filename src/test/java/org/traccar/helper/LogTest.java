package org.traccar.helper;

import org.junit.jupiter.api.Test;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LogTest {

    @Test
    public void testExceptionStack() {
        assertEquals(
                "test - Exception (LogTest:15 < ...)",
                Log.exceptionStack(new Exception("test")));
    }

    @Test
    public void testExceptionStackRootCause() {
        assertEquals(
                "root - Exception (LogTest:22 < ...)",
                Log.exceptionStack(new Exception("test", new Exception("root"))));
    }

    @Test
    public void testFormatToken() {
        var record = new LogRecord(Level.WARNING, "/api/session?token=secret&other=value");
        String formatted = new Log.LogFormatter(false).format(record);
        assertTrue(formatted.trim().endsWith("/api/session?token=[REDACTED]&other=value"));
    }

}
