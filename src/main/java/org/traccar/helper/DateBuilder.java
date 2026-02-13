/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateBuilder {

    private final Calendar calendar;

    public DateBuilder() {
        this(TimeZone.getTimeZone("UTC"));
    }

    public DateBuilder(Date time) {
        this(time, TimeZone.getTimeZone("UTC"));
    }

    public DateBuilder(TimeZone timeZone) {
        this(new Date(0), timeZone);
    }

    public DateBuilder(Date time, TimeZone timeZone) {
        calendar = Calendar.getInstance(timeZone);
        calendar.clear();
        calendar.setTimeInMillis(time.getTime());
    }

    public DateBuilder setYear(int year) {
        if (year < 100) {
            year += 2000;
        }
        calendar.set(Calendar.YEAR, year);
        return this;
    }

    public DateBuilder setMonth(int month) {
        calendar.set(Calendar.MONTH, month - 1);
        return this;
    }

    public DateBuilder setDay(int day) {
        calendar.set(Calendar.DAY_OF_MONTH, day);
        return this;
    }

    public DateBuilder setDate(int year, int month, int day) {
        return setYear(year).setMonth(month).setDay(day);
    }

    public DateBuilder setDateReverse(int day, int month, int year) {
        return setDate(year, month, day);
    }

    public DateBuilder setCurrentDate() {
        Calendar now = Calendar.getInstance(calendar.getTimeZone());
        return setYear(now.get(Calendar.YEAR))
                .setMonth(now.get(Calendar.MONTH) + 1)
                .setDay(now.get(Calendar.DAY_OF_MONTH));
    }

    public DateBuilder setHour(int hour) {
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        return this;
    }

    public DateBuilder setMinute(int minute) {
        calendar.set(Calendar.MINUTE, minute);
        return this;
    }

    public DateBuilder addMinute(int minute) {
        calendar.add(Calendar.MINUTE, minute);
        return this;
    }

    public DateBuilder setSecond(int second) {
        calendar.set(Calendar.SECOND, second);
        return this;
    }

    public DateBuilder addSeconds(long seconds) {
        calendar.setTimeInMillis(calendar.getTimeInMillis() + seconds * 1000);
        return this;
    }

    public DateBuilder setMillis(int millis) {
        calendar.set(Calendar.MILLISECOND, millis);
        return this;
    }

    public DateBuilder addMillis(long millis) {
        calendar.setTimeInMillis(calendar.getTimeInMillis() + millis);
        return this;
    }

    public DateBuilder setTime(int hour, int minute, int second) {
        return setHour(hour).setMinute(minute).setSecond(second);
    }

    public DateBuilder setTimeReverse(int second, int minute, int hour) {
        return setHour(hour).setMinute(minute).setSecond(second);
    }

    public DateBuilder setTime(int hour, int minute, int second, int millis) {
        return setHour(hour).setMinute(minute).setSecond(second).setMillis(millis);
    }

    public Date getDate() {
        return calendar.getTime();
    }

}
