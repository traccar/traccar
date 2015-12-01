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

import org.traccar.Context;
import org.traccar.model.User;

import java.sql.SQLException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

public class SecurityRequestFilter implements ContainerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    public static final String BASIC_REALM = "Basic realm=\"api\"";

    public static String[] decodeBasicAuth(String auth) {
        auth = auth.replaceFirst("[B|b]asic ", "");
        byte[] decodedBytes = DatatypeConverter.parseBase64Binary(auth);
        if (decodedBytes != null && decodedBytes.length > 0) {
            return new String(decodedBytes).split(":", 2);
        }
        return null;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        try {
            String[] auth = decodeBasicAuth(requestContext.getHeaderString(AUTHORIZATION_HEADER));
            User user = Context.getDataManager().login(auth[0], auth[1]);
            if (user != null) {
                requestContext.setSecurityContext(
                        new UserSecurityContext(new UserPrincipal(user.getId(), user.getName())));
            } else {
                throw new WebApplicationException(
                        Response.status(Response.Status.UNAUTHORIZED).header(WWW_AUTHENTICATE, BASIC_REALM).build());
            }
        } catch (SQLException e) {
            throw new WebApplicationException(e);
        }
    }

}
