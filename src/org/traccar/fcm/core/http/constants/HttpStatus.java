// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.core.http.constants;

public final class HttpStatus {

    private HttpStatus() {

    }

    public static final int CONTINUE = 100;

    public static final int SWITCHING_PROTOCOLS = 101;

    public static final int PROCESSING = 102;

    public static final int OK = 200;

    public static final int CREATED = 201;

    public static final int ACCEPTED = 202;

    public static final int NON_AUTHORITATIVE_INFORMATION = 203;

    public static final int NO_CONTENT = 204;

    public static final int RESET_CONTENT = 205;

    public static final int PARTIAL_CONTENT = 206;

    public static final int MULTI_STATUS = 207;

    public static final int MULTIPLE_CHOICES = 300;

    public static final int MOVED_PERMANENTLY = 301;

    public static final int MOVED_TEMPORARILY = 302;

    public static final int SEE_OTHER = 303;

    public static final int NOT_MODIFIED = 304;

    public static final int USE_PROXY = 305;

    public static final int TEMPORARY_REDIRECT = 307;

    public static final int BAD_REQUEST = 400;

    public static final int UNAUTHORIZED = 401;

    public static final int PAYMENT_REQUIRED = 402;

    public static final int FORBIDDEN = 403;

    public static final int NOT_FOUND = 404;

    public static final int METHOD_NOT_ALLOWED = 405;

    public static final int NOT_ACCEPTABLE = 406;

    public static final int PROXY_AUTHENTICATION_REQUIRED = 407;

    public static final int REQUEST_TIMEOUT = 408;

    public static final int CONFLICT = 409;

    public static final int GONE = 410;

    public static final int LENGTH_REQUIRED = 411;

    public static final int PRECONDITION_FAILED = 412;

    public static final int REQUEST_TOO_LONG = 413;

    public static final int REQUEST_URI_TOO_LONG = 414;

    public static final int UNSUPPORTED_MEDIA_TYPE = 415;

    public static final int REQUESTED_RANGE_NOT_SATISFIABLE = 416;

    public static final int EXPECTATION_FAILED = 417;

    public static final int INSUFFICIENT_SPACE_ON_RESOURCE = 419;

    public static final int METHOD_FAILURE = 420;

    public static final int UNPROCESSABLE_ENTITY = 422;

    public static final int LOCKED = 423;

    public static final int FAILED_DEPENDENCY = 424;

    public static final int INTERNAL_SERVER_ERROR = 500;

    public static final int NOT_IMPLEMENTED = 501;

    public static final int BAD_GATEWAY = 502;

    public static final int SERVICE_UNAVAILABLE = 503;

    public static final int GATEWAY_TIMEOUT = 504;

    public static final int HTTP_VERSION_NOT_SUPPORTED = 505;

    public static final int INSUFFICIENT_STORAGE = 507;

}

