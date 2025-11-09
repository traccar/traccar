/*
 * Copyright 2024 Socratec Telematic GmbH
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
package org.socratec.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the type of a logbook entry
 */
public enum LogbookEntryType {
    BUSINESS(1),
    PRIVATE(2);

    private final int value;

    LogbookEntryType(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    public static LogbookEntryType fromValue(int value) {
        for (LogbookEntryType type : LogbookEntryType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return BUSINESS; // Default fallback
    }
}
