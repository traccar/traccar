/*
 * Copyright 2015 - 2017 Anton Tananaev (anton@traccar.org)
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

    public Integer nextInt() {
        if (hasNext()) {
            return Integer.parseInt(next());
        } else {
            return null;
        }
    }

    public int nextInt(int defaultValue) {
        if (hasNext()) {
            return Integer.parseInt(next());
        } else {
            return defaultValue;
        }
    }

    public Integer nextHexInt() {
        if (hasNext()) {
            return Integer.parseInt(next(), 16);
        } else {
            return null;
        }
    }

    public int nextHexInt(int defaultValue) {
        if (hasNext()) {
            return Integer.parseInt(next(), 16);
        } else {
            return defaultValue;
        }
    }

    public Integer nextBinInt() {
        if (hasNext()) {
            return Integer.parseInt(next(), 2);
        } else {
            return null;
        }
    }

    public int nextBinInt(int defaultValue) {
        if (hasNext()) {
            return Integer.parseInt(next(), 2);
        } else {
            return defaultValue;
        }
    }

    public Long nextLong() {
        if (hasNext()) {
            return Long.parseLong(next());
        } else {
            return null;
        }
    }

    public Long nextHexLong() {
        if (hasNext()) {
            return Long.parseLong(next(), 16);
        } else {
            return null;
        }
    }

    public long nextLong(long defaultValue) {
        return nextLong(10, defaultValue);
    }

    public long nextLong(int radix, long defaultValue) {
        if (hasNext()) {
            return Long.parseLong(next(), radix);
        } else {
            return defaultValue;
        }
    }

    public Double nextDouble() {
        if (hasNext()) {
            return Double.parseDouble(next());
        } else {
            return null;
        }
    }

    public double nextDouble(double defaultValue) {
        if (hasNext()) {
            return Double.parseDouble(next());
        } else {
            return defaultValue;
        }
    }

    public enum CoordinateFormat {
        DEG_DEG,
        DEG_DEG_HEM,
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
            case DEG_DEG_HEM:
                coordinate = Double.parseDouble(next() + '.' + next());
                hemisphere = next();
                break;
            case DEG_HEM:
                coordinate = nextDouble(0);
                hemisphere = next();
                break;
            case DEG_MIN_MIN:
                coordinate = nextInt(0);
                coordinate += Double.parseDouble(next() + '.' + next()) / 60;
                break;
            case DEG_MIN_MIN_HEM:
                coordinate = nextInt(0);
                coordinate += Double.parseDouble(next() + '.' + next()) / 60;
                hemisphere = next();
                break;
            case HEM_DEG:
                hemisphere = next();
                coordinate = nextDouble(0);
                break;
            case HEM_DEG_MIN:
                hemisphere = next();
                coordinate = nextInt(0);
                coordinate += nextDouble(0) / 60;
                break;
            case HEM_DEG_MIN_HEM:
                hemisphere = next();
                coordinate = nextInt(0);
                coordinate += nextDouble(0) / 60;
                if (hasNext()) {
                    hemisphere = next();
                }
                break;
            case HEM_DEG_MIN_MIN:
                hemisphere = next();
                coordinate = nextInt(0);
                coordinate += Double.parseDouble(next() + '.' + next()) / 60;
                break;
            case DEG_MIN_HEM:
            default:
                coordinate = nextInt(0);
                coordinate += nextDouble(0) / 60;
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
        HMS,
        SMH,
        HMS_YMD,
        HMS_DMY,
        SMH_YMD,
        SMH_DMY,
        DMY_HMS,
        DMY_HMSS,
        YMD_HMS,
        YMD_HMSS,
    }

    public Date nextDateTime(DateTimeFormat format, String timeZone) {
        int year = 0, month = 0, day = 0;
        int hour, minute, second, millisecond = 0;

        switch (format) {
            case HMS:
                hour = nextInt(0);
                minute = nextInt(0);
                second = nextInt(0);
                break;
            case SMH:
                second = nextInt(0);
                minute = nextInt(0);
                hour = nextInt(0);
                break;
            case HMS_YMD:
                hour = nextInt(0);
                minute = nextInt(0);
                second = nextInt(0);
                year = nextInt(0);
                month = nextInt(0);
                day = nextInt(0);
                break;
            case HMS_DMY:
                hour = nextInt(0);
                minute = nextInt(0);
                second = nextInt(0);
                day = nextInt(0);
                month = nextInt(0);
                year = nextInt(0);
                break;
            case SMH_YMD:
                second = nextInt(0);
                minute = nextInt(0);
                hour = nextInt(0);
                year = nextInt(0);
                month = nextInt(0);
                day = nextInt(0);
                break;
            case SMH_DMY:
                second = nextInt(0);
                minute = nextInt(0);
                hour = nextInt(0);
                day = nextInt(0);
                month = nextInt(0);
                year = nextInt(0);
                break;
            case DMY_HMS:
            case DMY_HMSS:
                day = nextInt(0);
                month = nextInt(0);
                year = nextInt(0);
                hour = nextInt(0);
                minute = nextInt(0);
                second = nextInt(0);
                break;
            case YMD_HMS:
            case YMD_HMSS:
            default:
                year = nextInt(0);
                month = nextInt(0);
                day = nextInt(0);
                hour = nextInt(0);
                minute = nextInt(0);
                second = nextInt(0);
                break;
        }

        if (format == DateTimeFormat.YMD_HMSS || format == DateTimeFormat.DMY_HMSS) {
            millisecond = nextInt(0); // (ddd)
        }

        if (year >= 0 && year < 100) {
            year += 2000;
        }

        DateBuilder dateBuilder;
        if (format != DateTimeFormat.HMS && format != DateTimeFormat.SMH) {
            if (timeZone != null) {
                dateBuilder = new DateBuilder(TimeZone.getTimeZone(timeZone));
            } else {
                dateBuilder = new DateBuilder();
            }
            dateBuilder.setDate(year, month, day);
        } else {
            if (timeZone != null) {
                dateBuilder = new DateBuilder(new Date(), TimeZone.getTimeZone(timeZone));
            } else {
                dateBuilder = new DateBuilder(new Date());
            }
        }

        dateBuilder.setTime(hour, minute, second, millisecond);

        return dateBuilder.getDate();
    }

    public Date nextDateTime(DateTimeFormat format) {
        return nextDateTime(format, null);
    }

    public Date nextDateTime() {
        return nextDateTime(DateTimeFormat.YMD_HMS, null);
    }

}
