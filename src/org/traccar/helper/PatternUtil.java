/*
 * Copyright 2015 - 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

public final class PatternUtil {

    private PatternUtil() {
    }

    public static class MatchResult {
        private String pattern;
        private String matched;
        private String remaining;

        public String getPattern() {
            return this.pattern;
        }

        public String getMatched() {
            return  this.matched;
        }

        public String getRemaining() {
            return this.remaining;
        }
    }

    public static MatchResult checkPattern(String pattern, String input) {

        MatchResult result = new MatchResult();

        for (int i = 0; i < pattern.length(); i++) {
            try {
                Matcher matcher = Pattern.compile("(" + pattern.substring(0, i) + ").*").matcher(input);
                if (matcher.matches()) {
                    result.pattern = pattern.substring(0, i);
                    result.matched = matcher.group(1);
                    result.remaining = input.substring(matcher.group(1).length());
                }
            } catch (PatternSyntaxException error) {
                Log.warning(error);
            }
        }

        return result;
    }

}
