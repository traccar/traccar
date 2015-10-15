/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    private int position;
    private Matcher matcher;

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

    public boolean hasNext() {
        if (matcher.group(position) != null) {
            return true;
        } else {
            position++;
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

    public double nextDouble() {
        if (hasNext()) {
            return Double.parseDouble(next());
        } else {
            return 0.0;
        }
    }

    public enum CoordinateFormat {
        DEG_MIN_HEM,
        HEM_DEG
    }

    public double nextCoordinate(CoordinateFormat format) {
        double coordinate;
        String hemisphere;

        switch (format) {
            case HEM_DEG:
                hemisphere = next();
                coordinate = nextDouble();
                break;
            case DEG_MIN_HEM:
            default:
                coordinate = nextDouble();
                coordinate += nextDouble() / 60;
                hemisphere = next();
                break;
        }

        if (hemisphere != null && (hemisphere.equals("S") || hemisphere.equals("W"))) {
            coordinate = -coordinate;
        }

        return coordinate;
    }

    public double nextCoordinate() {
        return  nextCoordinate(CoordinateFormat.DEG_MIN_HEM);
    }

}
