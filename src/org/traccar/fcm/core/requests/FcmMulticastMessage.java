// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.traccar.fcm.core.model.options.FcmMessageOptions;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class FcmMulticastMessage<TPayload> extends FcmMessage<TPayload> {

    private final List<String> registrationIds;

    public FcmMulticastMessage(FcmMessageOptions options, List<String> registrationIds) {
        super(options);

        if (registrationIds == null) {
            throw new IllegalArgumentException("registrationIds");
        }

        this.registrationIds = registrationIds;
    }

    @JsonProperty("registration_ids")
    public List<String> getRegistrationIds() {
        return registrationIds;
    }

}
