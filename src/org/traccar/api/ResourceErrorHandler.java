/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.traccar.helper.Log;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.HashMap;
import java.util.Map;

public class ResourceErrorHandler implements ExceptionMapper<Exception> {

    private static final String KEY_MESSAGE = "message";
    private static final String KEY_DETAILS = "details";

    @Override
    public Response toResponse(Exception e) {
        Map<String, String> error = new HashMap<>();
        if (e instanceof WebApplicationException) {
            WebApplicationException webApplicationException = (WebApplicationException) e;
            return Response.status(webApplicationException.getResponse().getStatus()).entity(error).build();
        } else {
            error.put(KEY_MESSAGE, e.getMessage());
            error.put(KEY_DETAILS, Log.exceptionStack(e));
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
    }

}
