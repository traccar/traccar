// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.exceptions;

import org.joda.time.Duration;

/**
 * This Exception is thrown, when a message failed, but we are allowed to Retry it. You have to respect the Retry Delay
 * associated with this Exception, before you retry the Operation. You can use the RetryUtils to retry the operations.
 */
public class FcmRetryAfterException extends FcmException {

    private final Duration retryDelay;

    public FcmRetryAfterException(int httpStatusCode, String httpReasonPhrase, Duration retryDelay) {
        super(httpStatusCode, httpReasonPhrase);

        this.retryDelay = retryDelay;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }
}
