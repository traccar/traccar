/*
 * Copyright 2021 - 2022 Anton Tananaev (anton@traccar.org)
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

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.api.signature.TokenManager;
import org.traccar.helper.model.UserUtil;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.storage.StorageException;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Locale;

@Singleton
public class TextTemplateFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextTemplateFormatter.class);

    private final VelocityEngine velocityEngine;
    private final TokenManager tokenManager;

    @Inject
    public TextTemplateFormatter(VelocityEngine velocityEngine, TokenManager tokenManager) {
        this.velocityEngine = velocityEngine;
        this.tokenManager = tokenManager;
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

        return velocityContext;
    }

    public Template getTemplate(String name, String path) {

        String templateFilePath;
        Template template;

        try {
            templateFilePath = Paths.get(path, name + ".vm").toString();
            template = velocityEngine.getTemplate(templateFilePath, StandardCharsets.UTF_8.name());
        } catch (ResourceNotFoundException error) {
            LOGGER.warn("Notification template error", error);
            templateFilePath = Paths.get(path, "unknown.vm").toString();
            template = velocityEngine.getTemplate(templateFilePath, StandardCharsets.UTF_8.name());
        }
        return template;
    }

    public NotificationMessage formatMessage(VelocityContext velocityContext, String name, String templatePath) {
        StringWriter writer = new StringWriter();
        getTemplate(name, templatePath).merge(velocityContext, writer);
        return new NotificationMessage((String) velocityContext.get("subject"), writer.toString());
    }

}
