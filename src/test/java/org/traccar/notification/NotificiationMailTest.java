package org.traccar.notification;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class NotificiationMailTest {

    private static final String FROM = "notification@traccar.org";
    private static final String TO = "anton@traccar.org";

    private static final String BODY = "Test email body.";
    private static final String SUBJECT = "Test";

    private static final String SMTP_USERNAME = "username";
    private static final String SMTP_PASSWORD = "password";

    private static final String HOST = "email-smtp.us-west-2.amazonaws.com";

    private static final int PORT = 25;

    @Disabled
    @Test
    public void test() throws Exception {

        Properties props = System.getProperties();
        props.put("mail.transport.protocol", "smtps");
        props.put("mail.smtp.port", PORT);

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        Session session = Session.getInstance(props);

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(TO));
        msg.setSubject(SUBJECT);
        msg.setContent(BODY, "text/plain");

        Transport transport = session.getTransport();

        try {
            transport.connect(HOST, SMTP_USERNAME, SMTP_PASSWORD);
            transport.sendMessage(msg, msg.getAllRecipients());
        } finally {
            transport.close();
        }

    }

}
