/*
 * Copyright 2023 - 2025 Anton Tananaev (anton@traccar.org)
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
package org.traccar.reports.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.api.security.PermissionsService;
import org.traccar.mail.MailManager;
import org.traccar.model.User;
import org.traccar.notification.TextTemplateFormatter;
import org.traccar.storage.StorageException;

import jakarta.activation.DataHandler;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ReportMailer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportMailer.class);

    private final PermissionsService permissionsService;
    private final MailManager mailManager;
    private final TextTemplateFormatter textTemplateFormatter;

    @Inject
    public ReportMailer(
            PermissionsService permissionsService, MailManager mailManager,
            TextTemplateFormatter textTemplateFormatter) {
        this.permissionsService = permissionsService;
        this.mailManager = mailManager;
        this.textTemplateFormatter = textTemplateFormatter;
    }

    public void sendAsync(long userId, ReportExecutor executor) {
        new Thread(() -> {
            try {
                var stream = new ByteArrayOutputStream();
                executor.execute(stream);

                MimeBodyPart attachment = new MimeBodyPart();
                attachment.setFileName("report.xlsx");
                attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(
                        stream.toByteArray(), "application/octet-stream")));

                User user = permissionsService.getUser(userId);
                mailManager.sendMessage(user, false, "Report", "The report is in the attachment.", attachment);
            } catch (StorageException | IOException | MessagingException e) {
                LOGGER.warn("Email report failed", e);
            }
        }).start();
    }

    public void sendAsync(User user, String url) {
        new Thread(() -> {
            try {
                var velocityContext = textTemplateFormatter.prepareContext(permissionsService.getServer(), user);
                velocityContext.put("reportUrl", url);
                var fullMessage = textTemplateFormatter.formatMessage(velocityContext, "scheduledReport", false);
                mailManager.sendMessage(user, false, fullMessage.subject(), fullMessage.body());
            } catch (StorageException | MessagingException e) {
                LOGGER.warn("Email report failed", e);
            }
        }).start();
    }

}
