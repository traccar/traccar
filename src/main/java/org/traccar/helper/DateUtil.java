/*
 * Copyright 2016 - 2026 Anton Tananaev (anton@traccar.org)
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

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

public final class DateUtil {

    private DateUtil() {}

    private static final DateTimeFormatter LOCAL_DATE_TIME = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public static Date correctDay(Date guess) {
        return correctDate(new Date(), guess, Calendar.DAY_OF_MONTH);
    }

    public static Date correctYear(Date guess) {
        return correctDate(new Date(), guess, Calendar.YEAR);
    }

    public static Date correctDate(Date now, Date guess, int field) {

        if (guess.getTime() > now.getTime()) {
            Date previous = dateAdd(guess, field, -1);
            if (now.getTime() - previous.getTime() < guess.getTime() - now.getTime()) {
                return previous;
            }
        } else if (guess.getTime() < now.getTime()) {
            Date next = dateAdd(guess, field, 1);
            if (next.getTime() - now.getTime() < now.getTime() - guess.getTime()) {
                return next;
            }
        }

        return guess;
    }

    private static Date dateAdd(Date guess, int field, int amount) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(guess);
        calendar.add(field, amount);
        return calendar.getTime();
    }

    public static Date parseDate(String value) {
        return Date.from(Instant.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(value)));
    }

    public static Date parse(DateTimeFormatter formatter, String value) {
        return Date.from(Instant.from(formatter.parse(value)));
    }

    public static String formatDate(Date date) {
        return formatDate(date, true);
    }

    public static String formatDate(Date date, boolean zoned) {
        if (zoned) {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault()).format(date.toInstant());
        } else {
            return LOCAL_DATE_TIME.format(date.toInstant());
        }
    }

    public enum SummaryReportPeriod {
        NONE,
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
    }

    public static SummaryReportPeriod nextSummaryReportPeriod(String reportPeriod, boolean daily) {
        if (daily) {
            return SummaryReportPeriod.DAILY;
        }
        switch (reportPeriod) {
            case "daily" -> {
                return SummaryReportPeriod.DAILY;
            }
            case "weekly" -> {
                return SummaryReportPeriod.WEEKLY;
            }
            case "monthly" -> {
                return SummaryReportPeriod.MONTHLY;
            }
            case "yearly" -> {
                return SummaryReportPeriod.YEARLY;
            }
            default -> {}
        }
        return SummaryReportPeriod.NONE;
    }

    public static ZonedDateTime startOfZDTDay(ZonedDateTime date) {
        return date.truncatedTo(ChronoUnit.DAYS);
    }

    public static ZonedDateTime startOfZDTWeek(ZonedDateTime date) {
        return date.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
    }

    public static ZonedDateTime startOfZDTMonth(ZonedDateTime date) {
        return date.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
    }

    public static ZonedDateTime startOfZDTYear(ZonedDateTime date) {
        return date.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
    }
}
