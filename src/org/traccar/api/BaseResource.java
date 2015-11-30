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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.traccar.Context;
import org.traccar.helper.Clazz;
import org.traccar.model.User;

public class BaseResource<T, I> {

    private final Class<T> clazz = Clazz.getGenericArgumentType(getClass());

    @javax.ws.rs.core.Context
    private SecurityContext securityContext;

    public Collection<T> getEntities() {
        Collection<T> collection;
        try {
            collection = Context.getDataManager().get(clazz);
        } catch (SQLException e) {
            throw new WebApplicationException(ResponseBuilder.badRequest(e));
        }
        if (collection == null || collection.isEmpty()) {
            throw new WebApplicationException(ResponseBuilder.notFound());
        } else {
            return collection;
        }
    }

    public T getEntity(I id) {
        validateSecurityContext(User.ROLE_USER, id);
        T entity = Clazz.newInstance(clazz);
        try {
            Clazz.setId(entity, id);
            entity = Context.getDataManager().get(entity);
        } catch (Exception e) {
            throw new WebApplicationException(ResponseBuilder.badRequest(e));
        }
        if (entity == null) {
            throw new WebApplicationException(ResponseBuilder.notFound());
        } else {
            return entity;
        }
    }

    public Response postEntity(T entity) {
        try {
            Context.getDataManager().add(entity);
            return ResponseBuilder.ok(entity);
        } catch (Exception e) {
            return ResponseBuilder.badRequest(e);
        }
    }

    public Response putEntity(I id, T entity) {
        try {
            Clazz.setId(entity, id);
            Context.getDataManager().update(entity);
            return ResponseBuilder.ok(entity);
        } catch (Exception e) {
            return ResponseBuilder.badRequest(e);
        }
    }

    public Response deleteEntity(I id) {
        try {
            T entity = Clazz.newInstance(clazz);
            Clazz.setId(entity, id);
            Context.getDataManager().remove(entity);
            return ResponseBuilder.deleted();
        } catch (Exception e) {
            return ResponseBuilder.badRequest(e);
        }
    }

    private void validateSecurityContext(String role, I id) {
        UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
        if (!securityContext.isUserInRole(role) && !userPrincipal.getId().equals(id)) {
            throw new WebApplicationException(ResponseBuilder.forbidden());
        }
    }
}
