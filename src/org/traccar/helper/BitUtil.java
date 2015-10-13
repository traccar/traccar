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

public final class BitUtil {

    private BitUtil() {
    }

    public static boolean check(long number, int index) {
        return (number & (1 << index)) != 0;
    }

    public static int range(int number, int index) {
        return number >> index;
    }

    public static int range(int number, int index, int length) {
        return (number >> index) & ((1 << length) - 1);
    }

    public static long range(long number, int index) {
        return number >> index;
    }

    public static long range(long number, int index, int length) {
        return (number >> index) & ((1L << length) - 1);
    }

}
