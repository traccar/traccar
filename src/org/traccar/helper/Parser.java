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

import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    private int position;
    private final Matcher matcher;

    public Parser(Pattern pattern, String input) {
        matcher = pattern.matcher(input);
    }

    public boolean matches() {
        position = 1;
        return matcher.matches();
    }

    public boolean find() {
        position = 1;
        return matcher.find();
    }

    public void skip(int number) {
        position += number;
    }

    public boolean hasNext() {
        return hasNext(1);
    }

    public boolean hasNext(int number) {
        String value = matcher.group(position);
        if (value != null && !value.isEmpty()) {
            return true;
        } else {
            position += number;
            return false;
        }
    }

    public String next() {
        return matcher.group(position++);
    }

    public int nextInt() {
        return nextInt(10);
    }

    public int nextInt(int radix) {
        if (hasNext()) {
            return Integer.parseInt(next(), radix);
        } else {
            return 0;
        }
    }

    public long nextLong() {
        return nextLong(10);
    }

    public long nextLong(int radix) {
        if (hasNext()) {
            return Long.parseLong(next(), radix);
        } else {
            return 0;
        }
    }

    public double nextDouble() {
        if (hasNext()) {
            return Double.parseDouble(next());
        } else {
            return 0.0;
        }
    }

    public enum CoordinateFormat {
        DEG_DEG,
        DEG_HEM,
        DEG_MIN_MIN,
        DEG_MIN_HEM,
        DEG_MIN_MIN_HEM,
        HEM_DEG_MIN_MIN,
        HEM_DEG,
        HEM_DEG_MIN,
        HEM_DEG_MIN_HEM
    }

    public double nextCoordinate(CoordinateFormat format) {
        double coordinate;
        String hemisphere = null;

        switch (format) {
            case DEG_DEG:
                coordinate = Double.parseDouble(next() + '.' + next());
                break;
            case DEG_HEM:
                coordinate = nextDouble();
                hemisphere = next();
                break;
            case DEG_MIN_MIN:
                coordinate = nextInt();
                coordinate += Double.parseDouble(next() + '.' + next()) / 60;
                break;
            case DEG_MIN_MIN_HEM:
                coordinate = nextInt();
                coordinate += Double.parseDouble(next() + '.' + next()) / 60;
                hemisphere = next();
                break;
            case HEM_DEG:
                hemisphere = next();
                coordinate = nextDouble();
                break;
            case HEM_DEG_MIN:
                hemisphere = next();
                coordinate = nextInt();
                coordinate += nextDouble() / 60;
                break;
            case HEM_DEG_MIN_HEM:
                hemisphere = next();
                coordinate = nextInt();
                coordinate += nextDouble() / 60;
                if (hasNext()) {
                    hemisphere = next();
                }
                break;
            case HEM_DEG_MIN_MIN:
                hemisphere = next();
                coordinate = nextInt();
                coordinate += Double.parseDouble(next() + '.' + next()) / 60;
                break;
            case DEG_MIN_HEM:
            default:
                coordinate = nextInt();
                coordinate += nextDouble() / 60;
                hemisphere = next();
                break;
        }

        if (hemisphere != null && (hemisphere.equals("S") || hemisphere.equals("W") || hemisphere.equals("-"))) {
            coordinate = -Math.abs(coordinate);
        }

        return coordinate;
    }

    public double nextCoordinate() {
        return nextCoordinate(CoordinateFormat.DEG_MIN_HEM);
    }

    public enum DateTimeFormat {
        HMS,         // HHMMSS
        SMH,         // SSMMHH

        HMS_YMD,     // HHMMSSYYYYMMDD      or  HHMMSSYYMMDD
        HMS_DMY,     // HHMMSSDDMMYYYY      or  HHMMSSDDMMYY
        SMH_YMD,     // SSMMHHYYYYMMDD      or  SSMMHHYYMMDD
        SMH_DMY,     // SSMMHHDDMMYYYY      or  SSMMHHDDMMYY

        DMY_HMS,     // DDMMYYYYHHMMSS      or  DDMMYYHHMMSS
        DMY_HMSS,    // DDMMYYYYHHMMSS.sss  or  DDMMYYHHMMSS.sss
        YMD_HMS,     // YYYYMMDDHHMMSS      or  YYMMDDHHMMSS
        YMD_HMSS,    // YYYYMMDDHHMMSS.sss  or  YYMMDDHHMMSS.sss
    }

    private static final DateTimeFormat DEFAULT_FORMAT = DateTimeFormat.YMD_HMS;
    private static final String DEFAULT_TZ = "UTC";
    private static final int DEFAULT_RADIX = 10;

    public Date nextDateTime(DateTimeFormat format, String tz, int radix) {
        int year = 0, month = 0, day = 0;
        int hour = 0, minute = 0, second = 0, millisecond = 0;
        TimeZone timeZone = TimeZone.getTimeZone(tz);

        switch (format) {
            case HMS:
                hour = nextInt(radix); minute = nextInt(radix); second = nextInt(radix);
                break;
            case SMH:
                second = nextInt(radix); minute = nextInt(radix); hour = nextInt(radix);
                break;
            case HMS_YMD:
                hour = nextInt(radix); minute = nextInt(radix); second = nextInt(radix);
                year = nextInt(radix); month = nextInt(radix); day = nextInt(radix);
                break;
            case HMS_DMY:
                hour = nextInt(radix); minute = nextInt(radix); second = nextInt(radix);
                day = nextInt(radix); month = nextInt(radix); year = nextInt(radix);
                break;
            case SMH_YMD:
                second = nextInt(radix); minute = nextInt(radix); hour = nextInt(radix);
                year = nextInt(radix); month = nextInt(radix); day = nextInt(radix);
                break;
            case SMH_DMY:
                second = nextInt(radix); minute = nextInt(radix); hour = nextInt(radix);
                day = nextInt(radix); month = nextInt(radix); year = nextInt(radix);
                break;
            case DMY_HMS:
            case DMY_HMSS:
                day = nextInt(radix); month = nextInt(radix); year = nextInt(radix);
                hour = nextInt(radix); minute = nextInt(radix); second = nextInt(radix);
                break;
            case YMD_HMS:
            case YMD_HMSS:
            default:
                year = nextInt(radix); month = nextInt(radix); day = nextInt(radix);
                hour = nextInt(radix); minute = nextInt(radix); second = nextInt(radix);
                break;
        }

        if (format == DateTimeFormat.YMD_HMSS || format == DateTimeFormat.DMY_HMSS) {
                millisecond = nextInt(radix); // (ddd)
        }

        if (year >= 0 && year < 100) {
            year += 2000;
        }

        DateBuilder dateBuilder = new DateBuilder(timeZone);

        if (format != DateTimeFormat.HMS || format != DateTimeFormat.SMH) {
                dateBuilder.setDate(year, month, day);
        }

        dateBuilder.setTime(hour, minute, second, millisecond);

        return dateBuilder.getDate();
    }

    public Date nextDateTime(String tz, int radix) {
        return nextDateTime(DEFAULT_FORMAT, tz, radix);
    }

    public Date nextDateTime(DateTimeFormat format, int radix) {
        return nextDateTime(format, DEFAULT_TZ, radix);
    }

    public Date nextDateTime(DateTimeFormat format, String tz) {
        return nextDateTime(format, tz, DEFAULT_RADIX);
    }

    public Date nextDateTime(DateTimeFormat format) {
        return nextDateTime(format, DEFAULT_TZ, DEFAULT_RADIX);
    }

    public Date nextDateTime(String tz) {
        return nextDateTime(DEFAULT_FORMAT, tz, DEFAULT_RADIX);
    }

    public Date nextDateTime(int radix) {
        return nextDateTime(DEFAULT_FORMAT, DEFAULT_TZ, radix);
    }

    public Date nextDateTime() {
        return nextDateTime(DEFAULT_FORMAT, DEFAULT_TZ, DEFAULT_RADIX);
    }

}
