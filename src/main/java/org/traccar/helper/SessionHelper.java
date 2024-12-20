/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
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
package org.traccar.helper;

import org.traccar.model.User;
import java.util.Date;

import jakarta.servlet.http.HttpServletRequest;

public final class SessionHelper {

    public static final String USER_ID_KEY = "userId";
    public static final String EXPIRATION_KEY = "expiration";

    private SessionHelper() {
    }

    public static void userLogin(HttpServletRequest request, User user, Date expiration) {
        request.getSession().invalidate();
        request.getSession().setAttribute(USER_ID_KEY, user.getId());

        if (expiration != null) {
            request.getSession().setAttribute(EXPIRATION_KEY, expiration);
        }

        LogAction.login(user.getId(), WebHelper.retrieveRemoteAddress(request));
    }

}
