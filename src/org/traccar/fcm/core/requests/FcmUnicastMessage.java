// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.traccar.fcm.core.model.options.FcmMessageOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class FcmUnicastMessage<TPayload> extends FcmMessage<TPayload> {

    private final String to;

    public FcmUnicastMessage(FcmMessageOptions options, String to) {
        super(options);

        if (to == null) {
            throw new IllegalArgumentException("to");
        }

        this.to = to;
    }

    @JsonProperty("to")
    public String getTo() {
        return to;
    }

}
