// Copyright (c) Philipp Wagner. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package org.traccar.fcm.client.settings;

import org.traccar.fcm.core.http.options.IFcmClientSettings;

import java.util.Properties;

/**
 * Initializes Client Settings from Properties.
 */
public class PropertiesBasedSettings implements IFcmClientSettings {

    private final String fcmUrl;
    private final String fcmApiKey;

    public PropertiesBasedSettings(Properties properties) {
        fcmUrl = properties.getProperty("fcm.api.url", "https://fcm.googleapis.com/fcm/send");
        fcmApiKey = properties.getProperty("fcm.api.key");
    }

    @Override
    public String getFcmUrl() {
        return fcmUrl;
    }

    @Override
    public String getApiKey() {
        return fcmApiKey;
    }

}
