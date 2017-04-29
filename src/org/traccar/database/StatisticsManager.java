/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
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

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Statistics;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class StatisticsManager {

    private static final int SPLIT_MODE = Calendar.DAY_OF_MONTH;

    private int lastUpdate = Calendar.getInstance().get(SPLIT_MODE);

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
        if (lastUpdate != currentUpdate) {
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
                Context.getDataManager().addStatistics(statistics);
            } catch (SQLException e) {
                Log.warning(e);
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
            lastUpdate = currentUpdate;
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
