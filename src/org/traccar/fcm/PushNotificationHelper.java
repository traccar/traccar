package org.traccar.fcm;

import org.json.JSONObject;
import org.traccar.Context;
import org.traccar.helper.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public final class PushNotificationHelper {
    private static final String FCM_SERVER_TOKEN = Context.getConfig().getString("fcm.servertoken");
    private static final String FCM_URL = Context.getConfig().getString("fcm.url");
    private static final boolean FCM_DRY_RUN = Context.getConfig().getBoolean("fcm.dry_run");

    private PushNotificationHelper() {
    }

    public static void sendPushNotification(String deviceToken, String title, String body, int ttl) throws IOException {

        URL url = new URL(FCM_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "key=" + FCM_SERVER_TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");

        String notificationJsonString = buildNotificationJsonString(deviceToken, title, body, ttl);

        try {
            Log.debug("Sending FCM notification: " + notificationJsonString);

            OutputStreamWriter writerStream = new OutputStreamWriter(conn.getOutputStream());
            writerStream.write(notificationJsonString);
            writerStream.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder fcmResult = new StringBuilder();
            fcmResult.append("Result from FCM: ");

            String resultLine;
            while ((resultLine = reader.readLine()) != null) {
                fcmResult.append(System.lineSeparator());
                fcmResult.append(resultLine);
            }
            Log.debug(fcmResult.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String buildNotificationJsonString(String deviceToken, String title, String body, int ttl) {
        JSONObject json = new JSONObject();
        json.put("to", deviceToken.trim());
        json.put("priority", "high"); // Important for IoS apps that are closed
        json.put("dry_run", FCM_DRY_RUN);
        json.put("time_to_live", ttl);

        JSONObject info = new JSONObject();
        info.put("title", title);
        info.put("body", body);

        json.put("notification", info);

        return json.toString();
    }
}
