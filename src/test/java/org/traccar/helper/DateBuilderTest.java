package org.traccar.helper;

import org.junit.jupiter.api.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateBuilderTest {
    
    @Test
    public void testDateBuilder() throws ParseException {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        DateBuilder dateBuilder = new DateBuilder()
                .setDate(2015, 10, 20).setTime(1, 21, 11);

        assertEquals(dateFormat.parse("2015-10-20 01:21:11"), dateBuilder.getDate());

    }

}
