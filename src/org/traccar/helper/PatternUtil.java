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
import java.util.regex.PatternSyntaxException;

public class PatternUtil {

    public static String checkPattern(String pattern, String input) {

        String match = null;

        for (int i = 0; i < pattern.length(); i++) {
            try {
                Matcher matcher = Pattern.compile(pattern.substring(0, i) + ".*").matcher(input);
                if (matcher.matches()) {
                    match = pattern.substring(0, i);
                }
            } catch (PatternSyntaxException e) {
            }
        }

        return match;
    }

}
