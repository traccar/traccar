/*
 * Copyright 2016 - 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 - 2017 Andrey Kunitsyn (andrey@traccar.org)
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Calendar;
import org.traccar.model.CalendarPermission;

public class CalendarManager {

    private final DataManager dataManager;

    private final Map<Long, Calendar> calendars = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> userCalendars = new ConcurrentHashMap<>();

    public CalendarManager(DataManager dataManager) {
        this.dataManager = dataManager;
        refreshCalendars();
    }

    public final void refreshCalendars() {
        if (dataManager != null) {
            try {
                calendars.clear();
                for (Calendar calendar : dataManager.getCalendars()) {
                    calendars.put(calendar.getId(), calendar);
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
        refreshUserCalendars();
    }

    private Set<Long> getUserCalendarIds(long userId) {
        if (!userCalendars.containsKey(userId)) {
            userCalendars.put(userId, new HashSet<Long>());
        }
        return userCalendars.get(userId);
    }

    public Collection<Calendar> getUserCalendars(long userId) {
        ArrayList<Calendar> result = new ArrayList<>();
        for (long calendarId : getUserCalendarIds(userId)) {
            result.add(calendars.get(calendarId));
        }
        return result;
    }

    public Collection<Calendar> getManagedCalendars(long userId) {
        ArrayList<Calendar> result = new ArrayList<>();
        result.addAll(getUserCalendars(userId));
        for (long managedUserId : Context.getPermissionsManager().getUserPermissions(userId)) {
            result.addAll(getUserCalendars(managedUserId));
        }
        return result;
    }

    public final void refreshUserCalendars() {
        if (dataManager != null) {
            try {
                userCalendars.clear();
                for (CalendarPermission calendarsPermission : dataManager.getCalendarPermissions()) {
                    getUserCalendarIds(calendarsPermission.getUserId()).add(calendarsPermission.getCalendarId());
                }
            } catch (SQLException error) {
                Log.warning(error);
            }
        }
    }

    public Calendar getCalendar(long calendarId) {
        return calendars.get(calendarId);
    }

    public final void addCalendar(Calendar calendar) throws SQLException {
        dataManager.addCalendar(calendar);
        calendars.put(calendar.getId(), calendar);
    }

    public final void updateCalendar(Calendar calendar) throws SQLException {
        dataManager.updateCalendar(calendar);
        calendars.put(calendar.getId(), calendar);
    }

    public final void removeCalendar(long calendarId) throws SQLException {
        dataManager.removeCalendar(calendarId);
        calendars.remove(calendarId);
        refreshUserCalendars();
    }

    public Collection<Calendar> getAllCalendars() {
        return calendars.values();
    }

    public boolean checkCalendar(long userId, long calendarId) {
        return getUserCalendarIds(userId).contains(calendarId);
    }
}
