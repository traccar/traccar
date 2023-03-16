/*
 * Copyright 2022 - 2023 Anton Tananaev (anton@traccar.org)
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
package org.traccar.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.User;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

public class LogMailManager implements MailManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogMailManager.class);

    @Override
    public boolean getEmailEnabled() {
        return true;
    }

    @Override
    public void sendMessage(
            User user, boolean system, String subject, String body) throws MessagingException {
        sendMessage(user, system, subject, body, null);
    }

    @Override
    public void sendMessage(
            User user, boolean system, String subject, String body, MimeBodyPart attachment) throws MessagingException {
        LOGGER.info(
                "Email sent\nTo: {}\nSubject: {}\nAttachment: {}\nBody:\n{}",
                user.getEmail(), subject, attachment != null ? attachment.getFileName() : null, body);
    }

}
