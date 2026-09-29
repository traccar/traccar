/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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

public final class StringUtil {

    private StringUtil() {}

    public static boolean containsHex(String value) {
        for (char c : value.toCharArray()) {
            if (c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F') {
                return true;
            }
        }
        return false;
    }

    public static String stripLeading(char c, String value) {
        int start = 0;
        while (start < value.length() - 1 && value.charAt(start) == c) {
            start++;
        }
        return value.substring(start);
    }

    public static String stripTrailing(char c, String value) {
        int end = value.length();
        while (end > 1 && value.charAt(end - 1) == c) {
            end--;
        }
        return value.substring(0, end);
    }

}
