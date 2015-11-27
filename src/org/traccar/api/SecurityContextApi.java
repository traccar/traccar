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

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;

public class SecurityContextApi implements SecurityContext {

    private static final String AUTHENTICATION_SCHEME = "BASIC";
    private static final boolean IS_SECURE = true;

    private Principal userPrincipal;

    public SecurityContextApi(Principal userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

    public SecurityContextApi() {
    }

    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    @Override
    public boolean isUserInRole(String role) {
        UserPrincipal user = (UserPrincipal) userPrincipal;
        return user.getRoles().contains(role);
    }

    @Override
    public boolean isSecure() {
        return IS_SECURE;
    }

    @Override
    public String getAuthenticationScheme() {
        return AUTHENTICATION_SCHEME;
    }
}
