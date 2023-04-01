/*
 * Copyright 2017 - 2023 Anton Tananaev (anton@traccar.org)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.User;

public class OpenIDProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenIDProvider.class);

    private final Boolean force;
    private final String clientId;
    private final String authUrl;
    private final String tokenUrl;
    private final String userInfoUrl;
    private final String adminGroup;

    public OpenIDProvider(Config config) {
        force = config.getBoolean(Keys.OIDC_FORCE);
        clientId = config.getString(Keys.OIDC_CLIENTID);
        authUrl = config.getString(Keys.OIDC_AUTHURL);
        tokenUrl = config.getString(Keys.OIDC_TOKENURL);
        userInfoUrl = config.getString(Keys.OIDC_USERINFOURL);
        adminGroup = config.getString(Keys.OIDC_ADMINGROUP);
    }

}
