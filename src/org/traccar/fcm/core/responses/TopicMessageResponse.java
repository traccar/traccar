// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.traccar.fcm.core.model.enums.ErrorCodeEnum;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TopicMessageResponse {

    private final long messageId;
    private final ErrorCodeEnum errorCode;

    @JsonCreator
    public TopicMessageResponse(
            @JsonProperty("message_id") long messageId,
            @JsonProperty("error") ErrorCodeEnum errorCode) {
        this.messageId = messageId;
        this.errorCode = errorCode;
    }

    public long getMessageId() {
        return messageId;
    }

    public ErrorCodeEnum getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "TopicMessageResponse{" +
                "messageId=" + messageId +
                ", errorCode=" + errorCode +
                '}';
    }
}