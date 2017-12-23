// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.model.topics;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class TopicList {

    private final List<Topic> topics;

    public TopicList(List<Topic> topics) {
        this.topics = topics;
    }

    public List<Topic> getTopics() {
        return topics;
    }

    public String getTopicsCondition() {
        List<String> stringList = new ArrayList<>();
        for (Topic topic : topics) {
            stringList.add(String.format("'%s' in topics", topic.getName()));
        }
        return StringUtils.join(stringList, " || ");
       /* return topics.stream()
                .map(new Function<Topic, Object>() {
                    @Override
                    public Object apply(Topic topic) {
                        String.format("'%s' in topics", topic.getName())
                    }
                })
                .collect(Collectors.joining(" || "));*/
    }
}
