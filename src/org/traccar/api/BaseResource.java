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

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.traccar.Context;
import org.traccar.helper.Clazz;
import org.traccar.model.User;

public class BaseResource<T> {

    private static final String ERROR_KEY = "error";

    private final Class<T> clazz = Clazz.getGenericArgumentType(getClass());

    @javax.ws.rs.core.Context
    private SecurityContext securityContext;

    private static Map<String, String> getError(Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put(ERROR_KEY, e.getMessage());
        return error;
    }

    public Collection<T> getEntities() {
        Collection<T> collection;
        try {
            collection = Context.getDataManager().get(clazz);
        } catch (SQLException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(getError(e)).build());
        }
        if (collection == null || collection.isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        } else {
            return collection;
        }
    }

    public T getEntity(long id) {
        validateSecurityContext(User.ROLE_USER, id);
        T entity = Clazz.newInstance(clazz);
        try {
            Clazz.setId(entity, id);
            entity = Context.getDataManager().get(entity);
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity(getError(e)).build());
        }
        if (entity == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
        } else {
            return entity;
        }
    }

    public Response postEntity(T entity) {
        try {
            Context.getDataManager().add(entity);
            return Response.status(Response.Status.OK).entity(entity).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(getError(e)).build();
        }
    }

    public Response putEntity(long id, T entity) {
        try {
            Clazz.setId(entity, id);
            Context.getDataManager().update(entity);
            return Response.status(Response.Status.OK).entity(entity).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(getError(e)).build();
        }
    }

    public Response deleteEntity(long id) {
        try {
            T entity = Clazz.newInstance(clazz);
            Clazz.setId(entity, id);
            Context.getDataManager().remove(entity);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(getError(e)).build();
        }
    }

    private void validateSecurityContext(String role, long id) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        if (!securityContext.isUserInRole(role) && !userPrincipal.getId().equals(id)) {
            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).build());
        }
    }

}
