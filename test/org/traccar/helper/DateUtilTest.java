package org.traccar.helper;

import org.junit.Assert;
import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateUtilTest {
    
    @Test
    public void testCorrectDate() throws ParseException {

        DateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Assert.assertEquals(f.parse("2015-12-31 23:59:59"),
                DateUtil.correctDate(f.parse("2016-01-01 00:00:01"), f.parse("2016-01-01 23:59:59"), Calendar.DAY_OF_MONTH));

        Assert.assertEquals(f.parse("2016-01-01 00:00:02"),
                DateUtil.correctDate(f.parse("2016-01-01 00:00:01"), f.parse("2016-01-01 00:00:02"), Calendar.DAY_OF_MONTH));

        Assert.assertEquals(f.parse("2016-01-01 00:00:02"),
                DateUtil.correctDate(f.parse("2016-01-01 00:00:01"), f.parse("2015-12-31 00:00:02"), Calendar.DAY_OF_MONTH));

    }

}
