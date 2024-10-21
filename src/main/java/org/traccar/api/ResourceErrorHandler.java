/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.api;

import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.Log;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class ResourceErrorHandler implements ExceptionMapper<Exception> {

    private static final String HIDDEN_MESSAGE_KEY = "HiddenError";
    private boolean hideErrorDetail;

    @Inject
    public ResourceErrorHandler(Config config) {
        this.hideErrorDetail = config.getBoolean(Keys.WEB_HIDE_ERROR_DETAIL);
    }

    @Override
    public Response toResponse(Exception e) {
        if (e instanceof WebApplicationException webException) {
            String message = hideErrorDetail ? HIDDEN_MESSAGE_KEY : Log.exceptionStack(webException);
            return Response.fromResponse(webException.getResponse()).entity(message).build();
        } else {
            String message = hideErrorDetail ? HIDDEN_MESSAGE_KEY : Log.exceptionStack(e);
            return Response.status(Response.Status.BAD_REQUEST).entity(message).build();
        }
    }

}
