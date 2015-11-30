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
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.base64.Base64;
import org.jboss.netty.util.CharsetUtil;
import org.traccar.Context;
import org.traccar.model.User;

public final class AuthorizationBasic {

    private AuthorizationBasic() {
    }

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_SCHEME_VALUE = "Basic";
    public static final String REGEX = AUTHORIZATION_SCHEME_VALUE + " ";
    public static final String REPLACEMENT = "";
    public static final String TOKENIZER = ":";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String WWW_AUTHENTICATE_VALUE = "Basic realm=\"api\"";

    public static UserPrincipal getUserPrincipal(ContainerRequestContext requestContext) {
        final MultivaluedMap<String, String> headers = requestContext.getHeaders();
        final List<String> authorization = headers.get(AUTHORIZATION_HEADER);
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }
        final String encodedUsernameAndPassword = authorization.get(0).replaceFirst(REGEX, REPLACEMENT);
        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(encodedUsernameAndPassword, CharsetUtil.UTF_8);
        String usernameAndPassword = Base64.decode(buffer).toString(CharsetUtil.UTF_8);
        final StringTokenizer tokenizer = new StringTokenizer(usernameAndPassword, TOKENIZER);
        String username = tokenizer.nextToken();
        String password = tokenizer.nextToken();
        Set<String> roles = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        UserPrincipal userPrincipal = new UserPrincipal(username, password, roles);
        return userPrincipal;
    }

    public static boolean isAuthenticatedUser(UserPrincipal userPrincipal) {
        if (userPrincipal.getName() != null && userPrincipal.getPassword() != null) {
            User user;
            try {
                user = Context.getDataManager().login(userPrincipal.getName(), userPrincipal.getPassword());
            } catch (SQLException e) {
                return false;
            }
            if (user != null) {
                userPrincipal.setId(user.getId());
                /*
                for (Role role : user.getRoles()) {
                    userPrincipal.getRoles().add(role.getName());
                }
                */

                //Temporary solution
                userPrincipal.getRoles().add(ApplicationRole.USER);
                if (user.getAdmin()) {
                    userPrincipal.getRoles().add(ApplicationRole.ADMIN);
                }
                return true;
            }
        }
        return false;
    }

    public static boolean isAuthorizedUser(UserPrincipal userPrincipal, Set<String> roles) {
        for (String role : roles) {
            if (userPrincipal.getRoles().contains(role)) {
                return true;
            }
        }
        return false;
    }
}
