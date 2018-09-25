/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
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
package org.traccar.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.Statistics;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class StatisticsManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsManager.class);

    private static final int SPLIT_MODE = Calendar.DAY_OF_MONTH;

    private AtomicInteger lastUpdate = new AtomicInteger(Calendar.getInstance().get(SPLIT_MODE));

    private Set<Long> users = new HashSet<>();
    private Set<Long> devices = new HashSet<>();

    private int requests;
    private int messagesReceived;
    private int messagesStored;
    private int mailSent;
    private int smsSent;
    private int geocoderRequests;
    private int geolocationRequests;

    private void checkSplit() {
        int currentUpdate = Calendar.getInstance().get(SPLIT_MODE);
        if (lastUpdate.getAndSet(currentUpdate) != currentUpdate) {
            Statistics statistics = new Statistics();
            statistics.setCaptureTime(new Date());
            statistics.setActiveUsers(users.size());
            statistics.setActiveDevices(devices.size());
            statistics.setRequests(requests);
            statistics.setMessagesReceived(messagesReceived);
            statistics.setMessagesStored(messagesStored);
            statistics.setMailSent(mailSent);
            statistics.setSmsSent(smsSent);
            statistics.setGeocoderRequests(geocoderRequests);
            statistics.setGeolocationRequests(geolocationRequests);

            try {
                Context.getDataManager().addObject(statistics);
            } catch (SQLException e) {
                LOGGER.warn("Error saving statistics", e);
            }

            String url = Context.getConfig().getString("server.statistics");
            if (url != null) {
                String time = Context.DATE_FORMATTER.format(statistics.getCaptureTime().toInstant());

                Form form = new Form();
                form.param("version", Context.getAppVersion());
                form.param("captureTime", time);
                form.param("activeUsers", String.valueOf(statistics.getActiveUsers()));
                form.param("activeDevices", String.valueOf(statistics.getActiveDevices()));
                form.param("requests", String.valueOf(statistics.getRequests()));
                form.param("messagesReceived", String.valueOf(statistics.getMessagesReceived()));
                form.param("messagesStored", String.valueOf(statistics.getMessagesStored()));
                form.param("mailSent", String.valueOf(statistics.getMailSent()));
                form.param("smsSent", String.valueOf(statistics.getSmsSent()));
                form.param("geocoderRequests", String.valueOf(statistics.getGeocoderRequests()));
                form.param("geolocationRequests", String.valueOf(statistics.getGeolocationRequests()));

                Context.getClient().target(url).request().async().post(Entity.form(form));
            }

            users.clear();
            devices.clear();
            requests = 0;
            messagesReceived = 0;
            messagesStored = 0;
            mailSent = 0;
            smsSent = 0;
            geocoderRequests = 0;
            geolocationRequests = 0;
        }
    }

    public synchronized void registerRequest(long userId) {
        checkSplit();
        requests += 1;
        if (userId != 0) {
            users.add(userId);
        }
    }

    public synchronized void registerMessageReceived() {
        checkSplit();
        messagesReceived += 1;
    }

    public synchronized void registerMessageStored(long deviceId) {
        checkSplit();
        messagesStored += 1;
        if (deviceId != 0) {
            devices.add(deviceId);
        }
    }

    public synchronized void registerMail() {
        checkSplit();
        mailSent += 1;
    }

    public synchronized void registerSms() {
        checkSplit();
        smsSent += 1;
    }

    public synchronized void registerGeocoderRequest() {
        checkSplit();
        geocoderRequests += 1;
    }

    public synchronized void registerGeolocationRequest() {
        checkSplit();
        geolocationRequests += 1;
    }

}
