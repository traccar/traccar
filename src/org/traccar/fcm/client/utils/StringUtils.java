// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.client.utils;

public final class StringUtils {

    private StringUtils() {
    }

    /**
     * Returns true, if a string is null or only contains of Whitespace characters.
     *
     * @param input Input string
     * @return true, if string is null or a whitespace character
     */
    public static boolean isNullOrWhiteSpace(String input) {
        return input == null || input.trim().length() == 0;
    }
}