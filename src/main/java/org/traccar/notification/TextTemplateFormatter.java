/*
 * Copyright 2021 - 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.notification;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.api.signature.TokenManager;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.LocaleManager;
import org.traccar.helper.model.UserUtil;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.storage.StorageException;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Locale;

@Singleton
public class TextTemplateFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextTemplateFormatter.class);

    private final VelocityEngine velocityEngine;
    private final TokenManager tokenManager;
    private final LocaleManager localeManager;
    private final String templatesRoot;

    @Inject
    public TextTemplateFormatter(
            VelocityEngine velocityEngine, TokenManager tokenManager, LocaleManager localeManager, Config config) {
        this.velocityEngine = velocityEngine;
        this.tokenManager = tokenManager;
        this.localeManager = localeManager;
        templatesRoot = config.getString(Keys.TEMPLATES_ROOT);
    }

    public VelocityContext prepareContext(Server server, User user) {

        VelocityContext velocityContext = new VelocityContext();

        if (user != null) {
            velocityContext.put("user", user);
            velocityContext.put("timezone", UserUtil.getTimezone(server, user));
            try {
                velocityContext.put("token", tokenManager.generateToken(user.getId()));
            } catch (IOException | GeneralSecurityException | StorageException e) {
                LOGGER.warn("Token generation failed", e);
            }
        }

        velocityContext.put("webUrl", velocityEngine.getProperty("web.url"));
        velocityContext.put("dateTool", new DateTool());
        velocityContext.put("numberTool", new NumberTool());
        velocityContext.put("locale", Locale.getDefault());
        velocityContext.put("language", UserUtil.getLanguage(server, user));

        return velocityContext;
    }

    public NotificationMessage formatMessage(VelocityContext velocityContext, String name, boolean priority) {
        StringWriter writer = new StringWriter();
        String language = (String) velocityContext.get("language");
        Path filePath = localeManager.getTemplateFile(templatesRoot, "notifications", language, name + ".vm");
        if (filePath != null) {
            Template template = velocityEngine.getTemplate(filePath.toString(), StandardCharsets.UTF_8.name());
            template.merge(velocityContext, writer);
            return new NotificationMessage(
                    (String) velocityContext.get("subject"), (String) velocityContext.get("digest"),
                    writer.toString(), priority);
        } else {
            return new NotificationMessage("(***)", "(***)", "(***)", priority);
        }
    }

}
