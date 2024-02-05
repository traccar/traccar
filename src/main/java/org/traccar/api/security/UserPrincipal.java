/*
 * Copyright 2015 - 2023 Anton Tananaev (anton@traccar.org)
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
package org.traccar.api.security;

import java.security.Principal;
import java.util.Date;

public class UserPrincipal implements Principal {

    private final long userId;
    private final Date expiration;

    public UserPrincipal(long userId, Date expiration) {
        this.userId = userId;
        this.expiration = expiration;
    }

    public Long getUserId() {
        return userId;
    }

    public Date getExpiration() {
        return expiration;
    }

    @Override
    public String getName() {
        return null;
    }

}
