// Copyright (c) Philipp Wagner. All rights reserved.

// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.model.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ErrorCodeEnum {
    @JsonProperty("MissingRegistration")
    MissingRegistration,

    @JsonProperty("InvalidRegistration")
    InvalidRegistration,

    @JsonProperty("NotRegistered")
    NotRegistered,

    @JsonProperty("InvalidPackageName")
    InvalidPackageName,

    @JsonProperty("MismatchSenderId")
    MismatchSenderId,

    @JsonProperty("InvalidParameters")
    InvalidParameters,

    @JsonProperty("MessageTooBig")
    MessageTooBig,

    @JsonProperty("InvalidDataKey")
    InvalidDataKey,

    @JsonProperty("InvalidTtl")
    InvalidTtl,

    @JsonProperty("Unavailable")
    Unavailable,

    @JsonProperty("InternalServerError")
    InternalServerError,

    @JsonProperty("DeviceMessageRateExceeded")
    DeviceMessageRateExceeded,

    @JsonProperty("TopicsMessageRateExceeded")
    TopicsMessageRateExceeded,

    @JsonProperty("InvalidApnsCredential")
    InvalidApnsCredential
}
