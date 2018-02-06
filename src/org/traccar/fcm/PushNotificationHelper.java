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

    private PushNotificationHelper() {
    }

    public static void sendPushNotification(String deviceToken, String title, String body) throws IOException {

        URL url = new URL(FCM_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "key=" + FCM_SERVER_TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");

        String notificationJsonString = buildNotificationJsonString(deviceToken, title, body);

        try {
            OutputStreamWriter wr = new OutputStreamWriter(
                    conn.getOutputStream());
            wr.write(notificationJsonString);
            wr.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String resultLine;
            StringBuilder result = new StringBuilder();
            result.append("Result from FCM: ");
            while ((resultLine = br.readLine()) != null) {
                result.append('\n' + resultLine);
            }
            Log.info(result.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String buildNotificationJsonString(String deviceToken, String title, String body) {
        JSONObject json = new JSONObject();
        json.put("to", deviceToken.trim());
        json.put("priority", "high"); // Important for IoS apps that are closed

        JSONObject info = new JSONObject();
        info.put("title", title);
        info.put("body", body);

        json.put("notification", info);

        return json.toString();
    }
}
