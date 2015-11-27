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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.ext.Provider;

@Provider
public class SecurityRequestFilter implements ContainerRequestFilter {

    @javax.ws.rs.core.Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Method method = resourceInfo.getResourceMethod();

        //@PermitAll
        if (method.isAnnotationPresent(PermitAll.class)) {
            return;
        }

        //@DenyAll
        if (method.isAnnotationPresent(DenyAll.class)) {
            requestContext.abortWith(ResponseBuilder.forbidden());
            return;
        }

        //AuthorizationBasic
        UserPrincipal userPrincipal = AuthorizationBasic.getUserPrincipal(requestContext);
        if (userPrincipal == null
            || userPrincipal.getName() == null
            || userPrincipal.getPassword() == null
            || !isAuthenticatedUser(userPrincipal)) {
            requestContext.abortWith(ResponseBuilder.unauthorized());
            return;
        }

        //@RolesAllowed
        if (method.isAnnotationPresent(RolesAllowed.class)) {
            RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
            Set<String> roles = new HashSet<>(Arrays.asList(rolesAnnotation.value()));
            if (!isAuthorizedUser(userPrincipal, roles)) {
                requestContext.abortWith(ResponseBuilder.forbidden());
                return;
            }
        }

        //SecurityContext
        requestContext.setSecurityContext(new SecurityContextApi(userPrincipal));
    }

    private boolean isAuthenticatedUser(UserPrincipal principal) {
        return AuthorizationBasic.isAuthenticatedUser(principal);
    }

    private boolean isAuthorizedUser(UserPrincipal userPrincipal, Set<String> roles) {
        return AuthorizationBasic.isAuthorizedUser(userPrincipal, roles);
    }
}
