/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.sql.SQLException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.traccar.Config;
import org.traccar.Context;
import org.traccar.database.DataManager;
import org.traccar.helper.Log;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;

public final class NotificationMail {

    private NotificationMail() {
    }

    public static void sendMailSync(long userId, Event event, Position position) {
        Config config = Context.getConfig();
        DataManager dataManager = Context.getDataManager();

        Properties mailServerProperties;
        Session mailSession;
        MimeMessage mailMessage;

        String from = null;
        String username = null;
        String password = null;

        try {
        User user = dataManager.getUser(userId);

        mailServerProperties = new Properties();
        String host = config.getString("mail.smtp.host", null);
        if (host != null) {
            mailServerProperties.put("mail.smtp.host", host);
            mailServerProperties.put("mail.smtp.port", config.getString("mail.smtp.port", "25"));

            if (config.getBoolean("mail.smtp.starttls.enable")) {
                mailServerProperties.put("mail.smtp.starttls.enable",
                        config.getBoolean("mail.smtp.starttls.enable"));
            } else if (config.getBoolean("mail.smtp.ssl.enable")) {
                mailServerProperties.put("mail.smtp.socketFactory.port",
                        mailServerProperties.getProperty("mail.smtp.port"));
                mailServerProperties.put("mail.smtp.socketFactory.class",
                        "javax.net.ssl.SSLSocketFactory");
            }

            mailServerProperties.put("mail.smtp.auth", config.getBoolean("mail.smtp.auth"));
            username = config.getString("mail.smtp.username", null);
            password = config.getString("mail.smtp.password", null);
            from = config.getString("mail.smtp.from", null);
        } else if (user.getAttributes().containsKey("mail.smtp.host")) {
            mailServerProperties.put("mail.smtp.host", user.getAttributes().get("mail.smtp.host"));
            String port = (String) user.getAttributes().get("mail.smtp.port");
            mailServerProperties.put("mail.smtp.port", (port != null) ? port : "25");
            if (user.getAttributes().containsKey("mail.smtp.starttls.enable")) {
                boolean tls = Boolean.parseBoolean((String) user.getAttributes().get("mail.smtp.starttls.enable"));
                mailServerProperties.put("mail.smtp.starttls.enable", tls);
            } else if (user.getAttributes().containsKey("mail.smtp.ssl.enable")) {
                boolean ssl = Boolean.parseBoolean((String) user.getAttributes().get("mail.smtp.ssl.enable"));
                if (ssl) {
                    mailServerProperties.put("mail.smtp.socketFactory.port",
                            mailServerProperties.getProperty("mail.smtp.port"));
                    mailServerProperties.put("mail.smtp.socketFactory.class",
                            "javax.net.ssl.SSLSocketFactory");
                }
            }
            boolean auth = Boolean.parseBoolean((String) user.getAttributes().get("mail.smtp.auth"));
            mailServerProperties.put("mail.smtp.auth", auth);

            username = (String) user.getAttributes().get("mail.smtp.username");
            password = (String) user.getAttributes().get("mail.smtp.password");
            from = (String) user.getAttributes().get("mail.smtp.from");
        } else {
            return;
        }

        mailSession = Session.getDefaultInstance(mailServerProperties, null);

        mailMessage = new MimeMessage(mailSession);

        if (from != null) {
            mailMessage.setFrom(new InternetAddress(from));
        }
        mailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(
                Context.getDataManager().getUser(userId).getEmail()));
        mailMessage.setSubject(NotificationFormatter.formatTitle(userId, event, position));
        mailMessage.setText(NotificationFormatter.formatMessage(userId, event, position));

        Transport transport = mailSession.getTransport("smtp");
        transport.connect(mailServerProperties.getProperty("mail.smtp.host"), username, password);
        transport.sendMessage(mailMessage, mailMessage.getAllRecipients());
        transport.close();

        } catch (MessagingException | SQLException error) {
            Log.warning(error);
        }
    }

    public static void sendMailAsync(final long userId, final Event event, final Position position) {
        Runnable runnableSend = new Runnable() {
            public void run() {
                sendMailSync(userId, event, position);
            }
        };

        new Thread(runnableSend).start();
    }
}
