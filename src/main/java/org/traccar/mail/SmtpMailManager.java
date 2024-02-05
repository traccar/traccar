/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.mail;

import org.traccar.config.Config;
import org.traccar.config.ConfigKey;
import org.traccar.config.Keys;
import org.traccar.database.StatisticsManager;
import org.traccar.model.User;
import org.traccar.notification.PropertiesProvider;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

public final class SmtpMailManager implements MailManager {

    private static final String CONTENT_TYPE = "text/html; charset=utf-8";

    private final Config config;
    private final StatisticsManager statisticsManager;

    public SmtpMailManager(Config config, StatisticsManager statisticsManager) {
        this.config = config;
        this.statisticsManager = statisticsManager;
    }

    private static void copyBooleanProperty(
            Properties properties, PropertiesProvider provider, ConfigKey<Boolean> key) {
        Boolean value = provider.getBoolean(key);
        if (value != null) {
            properties.put(key.getKey(), String.valueOf(value));
        }
    }

    private static void copyStringProperty(
            Properties properties, PropertiesProvider provider, ConfigKey<String> key) {
        String value = provider.getString(key);
        if (value != null) {
            properties.put(key.getKey(), value);
        }
    }

    private static Properties getProperties(PropertiesProvider provider) {
        String host = provider.getString(Keys.MAIL_SMTP_HOST);
        if (host != null) {
            Properties properties = new Properties();

            properties.put(Keys.MAIL_TRANSPORT_PROTOCOL.getKey(), provider.getString(Keys.MAIL_TRANSPORT_PROTOCOL));
            properties.put(Keys.MAIL_SMTP_HOST.getKey(), host);
            properties.put(Keys.MAIL_SMTP_PORT.getKey(), String.valueOf(provider.getInteger(Keys.MAIL_SMTP_PORT)));

            copyBooleanProperty(properties, provider, Keys.MAIL_SMTP_STARTTLS_ENABLE);
            copyBooleanProperty(properties, provider, Keys.MAIL_SMTP_STARTTLS_REQUIRED);
            copyBooleanProperty(properties, provider, Keys.MAIL_SMTP_SSL_ENABLE);
            copyStringProperty(properties, provider, Keys.MAIL_SMTP_SSL_TRUST);
            copyStringProperty(properties, provider, Keys.MAIL_SMTP_SSL_PROTOCOLS);
            copyStringProperty(properties, provider, Keys.MAIL_SMTP_USERNAME);
            copyStringProperty(properties, provider, Keys.MAIL_SMTP_PASSWORD);
            copyStringProperty(properties, provider, Keys.MAIL_SMTP_FROM);
            copyStringProperty(properties, provider, Keys.MAIL_SMTP_FROM_NAME);

            return properties;
        }
        return null;
    }

    public boolean getEmailEnabled() {
        return config.hasKey(Keys.MAIL_SMTP_HOST);
    }

    @Override
    public void sendMessage(
            User user, boolean system, String subject, String body) throws MessagingException {
        sendMessage(user, system, subject, body, null);
    }

    @Override
    public void sendMessage(
            User user, boolean system, String subject, String body, MimeBodyPart attachment) throws MessagingException {

        Properties properties = null;
        if (!config.getBoolean(Keys.MAIL_SMTP_IGNORE_USER_CONFIG)) {
            properties = getProperties(new PropertiesProvider(user));
        }
        if (properties == null && (system || !config.getBoolean(Keys.MAIL_SMTP_SYSTEM_ONLY))) {
            properties = getProperties(new PropertiesProvider(config));
        }
        if (properties == null) {
            throw new MessagingException("No SMTP configuration found");
        }

        Session session = Session.getInstance(properties);

        MimeMessage message = new MimeMessage(session);

        String from = properties.getProperty(Keys.MAIL_SMTP_FROM.getKey());
        if (from != null) {
            String fromName = properties.getProperty(Keys.MAIL_SMTP_FROM_NAME.getKey());
            if (fromName != null) {
                try {
                    message.setFrom(new InternetAddress(from, fromName));
                } catch (UnsupportedEncodingException e) {
                    throw new MessagingException("Email address issue");
                }
            } else {
                message.setFrom(new InternetAddress(from));
            }
        }

        message.addRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));
        message.setSubject(subject);
        message.setSentDate(new Date());

        if (attachment != null) {
            Multipart multipart = new MimeMultipart();

            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(body, CONTENT_TYPE);
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachment);

            message.setContent(multipart);
        } else {
            message.setContent(body, CONTENT_TYPE);
        }

        try (Transport transport = session.getTransport()) {
            statisticsManager.registerMail();
            transport.connect(
                    properties.getProperty(Keys.MAIL_SMTP_HOST.getKey()),
                    properties.getProperty(Keys.MAIL_SMTP_USERNAME.getKey()),
                    properties.getProperty(Keys.MAIL_SMTP_PASSWORD.getKey()));
            transport.sendMessage(message, message.getAllRecipients());
        }
    }

}
