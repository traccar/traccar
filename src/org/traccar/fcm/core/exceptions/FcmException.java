// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.exceptions;

public abstract class FcmException extends RuntimeException {

    private final int httpStatusCode;
    private final String reasonPhrase;

    public FcmException(int httpStatusCode, String reasonPhrase) {
        this.httpStatusCode = httpStatusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }
}
