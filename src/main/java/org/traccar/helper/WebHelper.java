/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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

import java.net.InetAddress;
import java.net.UnknownHostException;

import jakarta.servlet.http.HttpServletRequest;

import org.eclipse.jetty.util.URIUtil;
import org.traccar.config.Config;
import org.traccar.config.Keys;

public final class WebHelper {

    private WebHelper() {
    }

    public static String retrieveRemoteAddress(HttpServletRequest request) {

        if (request != null) {
            String remoteAddress = request.getHeader("X-FORWARDED-FOR");

            if (remoteAddress != null && !remoteAddress.isEmpty()) {
                int separatorIndex = remoteAddress.indexOf(",");
                if (separatorIndex > 0) {
                    return remoteAddress.substring(0, separatorIndex); // remove the additional data
                } else {
                    return remoteAddress;
                }
            } else {
                return request.getRemoteAddr();
            }
        } else {
            return null;
        }
    }

    public static String retrieveWebUrl(Config config) {
        if (config.hasKey(Keys.WEB_URL)) {
            return config.getString(Keys.WEB_URL).replaceAll("/$", "");
        } else {
            String address;
            try {
                address = config.getString(Keys.WEB_ADDRESS, InetAddress.getLocalHost().getHostAddress());
            } catch (UnknownHostException e) {
                address = "localhost";
            }
            return URIUtil.newURI("http", address, config.getInteger(Keys.WEB_PORT), "", "");
        }
    }
}
