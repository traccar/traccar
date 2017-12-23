// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.model.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PriorityEnum {

    @JsonProperty("normal")
    Normal,

    @JsonProperty("high")
    High

}
