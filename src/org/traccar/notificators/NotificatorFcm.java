package org.traccar.notificators;

import org.traccar.Context;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.FullMessage;
import org.traccar.notification.MessageException;
import org.traccar.notification.NotificationFormatter;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;

public class NotificatorFcm extends Notificator {
    @Override
    public void sendSync(long userId, Event event, Position position) throws MessageException, InterruptedException {
        User user = Context.getPermissionsManager().getUser(userId);

        String serverKey = Context.getConfig().getString("notificator.fcm.serverkey", "");

        String url = "https://fcm.googleapis.com/fcm/send";

        Invocation.Builder requestBuilder = Context.getClient().target(url).request();
        requestBuilder = requestBuilder.header("Authorization", "key=" + serverKey);
        requestBuilder = requestBuilder.header("Content-Type", "application/json");

        AsyncInvoker invoker = requestBuilder.async();

        FullMessage fullMessage = NotificationFormatter.formatFCMMessage(userId, event, position);

        invoker.post(Entity.json(new FCM(user.getFcm(), new FCM.Notification(fullMessage.getBody(), fullMessage.getSubject() + " Alert"))));

    }

    public static class FCM {
        private String to;
        private Notification notification;

        public FCM() {
        }

        public FCM(String to, Notification notification) {
            this.to = to;
            this.notification = notification;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public Notification getNotification() {
            return notification;
        }

        public void setNotification(Notification notification) {
            this.notification = notification;
        }

        public static class Notification {
            private String body, title;

            public Notification() {
            }

            public Notification(String body, String title) {
                this.body = body;
                this.title = title;
            }

            public String getBody() {
                return body;
            }

            public void setBody(String body) {
                this.body = body;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }
        }
    }
}
