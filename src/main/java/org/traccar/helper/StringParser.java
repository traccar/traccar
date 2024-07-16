/*
 * Copyright 2015 - 2024 Joaquim Cardeira (joaquim.cardeira@gmail.com)
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

public class StringParser {
    private int index;
    private final String[] values;
    public StringParser(String[] values) {
        this.values = values;
    }
    public Integer nextInt() {
        return index++ < values.length && !values[index - 1].isEmpty() ? Integer.parseInt(values[index - 1]) : null;
    }

    public Double nextDouble() {
        return index++ < values.length && !values[index - 1].isEmpty() ? Double.parseDouble(values[index - 1]) : null;
    }

    public String next() {
        return index++ < values.length && !values[index - 1].isEmpty() ? values[index - 1] : null;
    }

    public Long nextHexLong() {
        return index++ < values.length && !values[index - 1].isEmpty() ? Long.parseLong(values[index - 1], 16) : null;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
