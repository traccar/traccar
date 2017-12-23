// Copyright (c) Philipp Wagner. All rights reserved.

// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.http.exceptions;

public class HttpCommunicationException extends RuntimeException {

    public HttpCommunicationException() {
    }

    public HttpCommunicationException(String message) {
        super(message);
    }

    public HttpCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpCommunicationException(Throwable cause) {
        super(cause);
    }

    public HttpCommunicationException(String message, Throwable cause,
                                      boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
