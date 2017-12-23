package org.traccar.fcm.client.utils;


import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public final class DateUtils {

    private DateUtils(){

    }
    /**
     * Gets the current UTC DateTime.
     *
     * @return Current UTC DateTime
     */
    public static DateTime getUtcNow() {
        return DateTime.now(DateTimeZone.UTC);
    }

}
