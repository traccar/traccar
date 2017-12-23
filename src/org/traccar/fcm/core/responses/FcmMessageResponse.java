// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FcmMessageResponse {

    private final long multicastId;
    private final int numberOfSuccess;
    private final int numberOfFailure;
    private final int numberOfCanonicalIds;
    private final List<FcmMessageResultItem> results;

    @JsonCreator
    public FcmMessageResponse(
            @JsonProperty("multicast_id") long multicastId,
            @JsonProperty("success") int numberOfSuccess,
            @JsonProperty("failure") int numberOfFailure,
            @JsonProperty("canonical_ids") int numberOfCanonicalIds,
            @JsonProperty("results") List<FcmMessageResultItem> results) {
        this.multicastId = multicastId;
        this.numberOfSuccess = numberOfSuccess;
        this.numberOfFailure = numberOfFailure;
        this.numberOfCanonicalIds = numberOfCanonicalIds;
        this.results = results;
    }

    public long getMulticastId() {
        return multicastId;
    }

    public int getNumberOfSuccess() {
        return numberOfSuccess;
    }

    public int getNumberOfFailure() {
        return numberOfFailure;
    }

    public int getNumberOfCanonicalIds() {
        return numberOfCanonicalIds;
    }

    public List<FcmMessageResultItem> getResults() {
        return results;
    }

}
