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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;

public final class ResponseBuilder implements Serializable {

    private static final long serialVersionUID = -2348334499023022836L;

    private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String BASIC_REALM = "Basic realm=\"api\"";
    private static final String ERROR = "error";

    private ResponseBuilder() {
    }

    public static Response ok() {
        return Response.status(Response.Status.OK).build();
    }

    public static <T> Response ok(T entity) {
        return Response.status(Response.Status.OK).entity(entity).build();
    }

    public static <T> Response ok(Collection<T> entities) {
        return Response.ok(entities).build();
    }

    public static Response created() {
        return Response.status(Response.Status.CREATED).build();
    }

    public static <T> Response created(T entity) {
        return Response.status(Response.Status.CREATED).entity(entity).build();
    }

    public static Response accepted() {
        return Response.status(Response.Status.ACCEPTED).build();
    }

    public static <T> Response accepted(T entity) {
        return Response.status(Response.Status.ACCEPTED).entity(entity).build();
    }

    public static Response deleted() {
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    public static Response notModified() {
        return Response.status(Response.Status.NOT_MODIFIED).build();
    }

    public static Response badRequest() {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    public static Response badRequest(Exception e) {
        return Response.status(Response.Status.BAD_REQUEST).entity(getError(e)).build();
    }

    public static Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED).header(WWW_AUTHENTICATE, BASIC_REALM).build();
    }

    public static Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).entity(getError(Response.Status.FORBIDDEN.name())).build();
    }

    public static Response notFound() {
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    public static Response timeout() {
        return Response.status(Response.Status.REQUEST_TIMEOUT).build();
    }

    public static Response conflict() {
        return Response.status(Response.Status.CONFLICT).build();
    }

    public static Response conflict(Exception e) {
        return Response.status(Response.Status.CONFLICT).entity(getError(e)).build();
    }

    public static Response notImplemented() {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    public static Response redirect(String uri) {
        try {
            return Response.seeOther(new URI(uri)).build();
        } catch (URISyntaxException e) {
            Logger.getAnonymousLogger().warning(e.getMessage());
            return null;
        }
    }

    private static Map<String, String> getError(Exception e) {
        return getError(e.getMessage());
    }

    private static Map<String, String> getError(String message) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR, message);
        return error;
    }

}
