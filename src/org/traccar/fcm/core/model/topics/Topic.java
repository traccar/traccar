// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.model.topics;

public class Topic {

    private final String name;

    public Topic(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getTopicPath() {
        return String.format("/%s/%s", "topics", name);
    }
}
