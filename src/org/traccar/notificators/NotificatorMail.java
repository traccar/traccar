/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 - 2018 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.notificators;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.FullMessage;
import org.traccar.notification.MessageException;
import org.traccar.notification.NotificationFormatter;
import org.traccar.notification.PropertiesProvider;

public final class NotificatorMail extends Notificator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificatorMail.class);

    private static Properties getProperties(PropertiesProvider provider) {
        Properties properties = new Properties();
        String host = provider.getString("mail.smtp.host");
        if (host != null) {
            properties.put("mail.transport.protocol", provider.getString("mail.transport.protocol", "smtp"));
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.port", String.valueOf(provider.getInteger("mail.smtp.port", 25)));

            Boolean starttlsEnable = provider.getBoolean("mail.smtp.starttls.enable");
            if (starttlsEnable != null) {
                properties.put("mail.smtp.starttls.enable", String.valueOf(starttlsEnable));
            }
            Boolean starttlsRequired = provider.getBoolean("mail.smtp.starttls.required");
            if (starttlsRequired != null) {
                properties.put("mail.smtp.starttls.required", String.valueOf(starttlsRequired));
            }

            Boolean sslEnable = provider.getBoolean("mail.smtp.ssl.enable");
            if (sslEnable != null) {
                properties.put("mail.smtp.ssl.enable", String.valueOf(sslEnable));
            }
            String sslTrust = provider.getString("mail.smtp.ssl.trust");
            if (sslTrust != null) {
                properties.put("mail.smtp.ssl.trust", sslTrust);
            }

            String sslProtocols = provider.getString("mail.smtp.ssl.protocols");
            if (sslProtocols != null) {
                properties.put("mail.smtp.ssl.protocols", sslProtocols);
            }

            String username = provider.getString("mail.smtp.username");
            if (username != null) {
                properties.put("mail.smtp.username", username);
            }
            String password = provider.getString("mail.smtp.password");
            if (password != null) {
                properties.put("mail.smtp.password", password);
            }
            String from = provider.getString("mail.smtp.from");
            if (from != null) {
                properties.put("mail.smtp.from", from);
            }
        }
        return properties;
    }

    @Override
    public void sendSync(long userId, Event event, Position position) throws MessageException {
        User user = Context.getPermissionsManager().getUser(userId);

        Properties properties = null;
        if (!Context.getConfig().getBoolean("mail.smtp.ignoreUserConfig")) {
            properties = getProperties(new PropertiesProvider(user));
        }
        if (properties == null || !properties.containsKey("mail.smtp.host")) {
            properties = getProperties(new PropertiesProvider(Context.getConfig()));
        }
        if (!properties.containsKey("mail.smtp.host")) {
            LOGGER.warn("No SMTP configuration found");
            return;
        }

        Session session = Session.getInstance(properties);

        MimeMessage message = new MimeMessage(session);

        try {
            String from = properties.getProperty("mail.smtp.from");
            if (from != null) {
                message.setFrom(new InternetAddress(from));
            }

            message.addRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));
            FullMessage fullMessage = NotificationFormatter.formatFullMessage(userId, event, position);
            message.setSubject(fullMessage.getSubject());
            message.setSentDate(new Date());
            message.setContent(fullMessage.getBody(), "text/html; charset=utf-8");

            Transport transport = session.getTransport();
            try {
                Context.getStatisticsManager().registerMail();
                transport.connect(
                        properties.getProperty("mail.smtp.host"),
                        properties.getProperty("mail.smtp.username"),
                        properties.getProperty("mail.smtp.password"));
                transport.sendMessage(message, message.getAllRecipients());
            } finally {
                transport.close();
            }
        } catch (MessagingException e) {
            throw new MessageException(e);
        }
    }

}
