// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.exceptions;

import org.traccar.fcm.core.http.constants.HttpStatus;

/**
 * This Exception is thrown, if a Bad Request to FCM was made.
 */
public class FcmBadRequestException extends FcmException {

    public FcmBadRequestException(String httpReasonPhrase) {
        super(HttpStatus.BAD_REQUEST, httpReasonPhrase);
    }


}
