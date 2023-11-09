package org.traccar.calendar;

import net.fortuna.ical4j.data.ParserException;
import org.junit.jupiter.api.Test;
import org.traccar.model.Calendar;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CalendarTest {
    
    @Test
    public void testCalendar() throws IOException, ParserException, ParseException, SQLException {
        String calendarString = "BEGIN:VCALENDAR\n" + 
                "PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN\n" + 
                "VERSION:2.0\n" + 
                "BEGIN:VTIMEZONE\n" + 
                "TZID:Asia/Yekaterinburg\n" + 
                "BEGIN:STANDARD\n" + 
                "TZOFFSETFROM:+0500\n" + 
                "TZOFFSETTO:+0500\n" + 
                "TZNAME:YEKT\n" + 
                "DTSTART:19700101T000000\n" + 
                "END:STANDARD\n" + 
                "END:VTIMEZONE\n" + 
                "BEGIN:VEVENT\n" + 
                "CREATED:20161213T045151Z\n" + 
                "LAST-MODIFIED:20161213T045242Z\n" + 
                "DTSTAMP:20161213T045242Z\n" + 
                "UID:9d000df0-6354-479d-a407-218dac62c7c9\n" + 
                "SUMMARY:Every night\n" + 
                "RRULE:FREQ=DAILY\n" + 
                "DTSTART;TZID=Asia/Yekaterinburg:20161130T230000\n" + 
                "DTEND;TZID=Asia/Yekaterinburg:20161201T070000\n" + 
                "TRANSP:OPAQUE\n" + 
                "END:VEVENT\n" + 
                "END:VCALENDAR";
        Calendar calendar = new Calendar();
        calendar.setData(calendarString.getBytes());
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssX");

        assertFalse(calendar.checkMoment(format.parse("2016-12-13 22:59:59+05")));
        assertTrue(calendar.checkMoment(format.parse("2016-12-13 23:00:01+05")));
        assertTrue(calendar.checkMoment(format.parse("2016-12-13 06:59:59+05")));
        assertFalse(calendar.checkMoment(format.parse("2016-12-13 07:00:01+05")));

        var periods = calendar.findPeriods(format.parse("2016-12-13 06:59:59+05"));
        assertFalse(periods.isEmpty());
    }
}
