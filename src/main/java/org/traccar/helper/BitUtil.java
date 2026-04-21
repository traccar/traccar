/*
 * Copyright 2015 - 2022 Anton Tananaev (anton@traccar.org)
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

public final class BitUtil {

    private BitUtil() {
    }

    public static boolean check(long number, int index) {
        return (number & (1L << index)) != 0;
    }

    public static int between(int number, int from, int to) {
        return (number >> from) & ((1 << to - from) - 1);
    }

    public static int from(int number, int from) {
        return number >> from;
    }

    public static int to(int number, int to) {
        return between(number, 0, to);
    }

    public static long between(long number, int from, int to) {
        return (number >> from) & ((1L << to - from) - 1L);
    }

    public static long from(long number, int from) {
        return number >> from;
    }

    public static long to(long number, int to) {
        return between(number, 0, to);
    }

}
