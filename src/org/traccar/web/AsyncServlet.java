/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.web;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.traccar.Context;
import org.traccar.GlobalTimer;
import org.traccar.database.ConnectionManager;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public class AsyncServlet extends BaseServlet {

    private static final long ASYNC_TIMEOUT = 120000;

    @Override
    protected boolean handle(String command, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        async(req.startAsync(), getUserId(req));
        return true;
    }

    public static class AsyncSession {

        private static final boolean DEBUG_ASYNC = false;

        private static final long SESSION_TIMEOUT = 30;
        private static final long REQUEST_TIMEOUT = 20;

        private boolean destroyed;
        private final long userId;
        private final Set<Long> devices = new HashSet<>();
        private Timeout sessionTimeout;
        private Timeout requestTimeout;
        private final Map<Long, Position> positions = new HashMap<>();
        private AsyncContext activeContext;

        private void logEvent(String message) {
            if (DEBUG_ASYNC) {
                Log.debug("AsyncSession: " + this.hashCode() + " destroyed: " + destroyed + " " + message);
            }
        }

        public AsyncSession(long userId, Collection<Long> devices) {
            logEvent("create userId: " + userId + " devices: " + devices.size());
            this.userId = userId;
            this.devices.addAll(devices);

            Collection<Position> initialPositions = Context.getConnectionManager().getInitialState(devices);
            for (Position position : initialPositions) {
                positions.put(position.getDeviceId(), position);
            }

            Context.getConnectionManager().addListener(devices, dataListener);
        }

        public boolean hasDevice(long deviceId) {
            return devices.contains(deviceId);
        }

        private final ConnectionManager.DataCacheListener dataListener = new ConnectionManager.DataCacheListener() {
            @Override
            public void onUpdate(Position position) {
                synchronized (AsyncSession.this) {
                    logEvent("onUpdate deviceId: " + position.getDeviceId());
                    if (!destroyed) {
                        if (requestTimeout != null) {
                            requestTimeout.cancel();
                            requestTimeout = null;
                        }
                        positions.put(position.getDeviceId(), position);
                        if (activeContext != null) {
                            response();
                        }
                    }
                }
            }
        };

        private final TimerTask sessionTimer = new TimerTask() {
            @Override
            public void run(Timeout tmt) throws Exception {
                synchronized (AsyncSession.this) {
                    logEvent("sessionTimeout");
                    destroyed = true;
                }
                Context.getConnectionManager().removeListener(devices, dataListener);
                synchronized (ASYNC_SESSIONS) {
                    ASYNC_SESSIONS.remove(userId);
                }
            }
        };

        private final TimerTask requestTimer = new TimerTask() {
            @Override
            public void run(Timeout tmt) throws Exception {
                synchronized (AsyncSession.this) {
                    logEvent("requestTimeout");
                    if (!destroyed && activeContext != null) {
                        response();
                    }
                }
            }
        };

        public synchronized void request(AsyncContext context) {
            logEvent("request context: " + context.hashCode());
            if (!destroyed) {
                activeContext = context;
                if (sessionTimeout != null) {
                    sessionTimeout.cancel();
                    sessionTimeout = null;
                }

                if (!positions.isEmpty()) {
                    response();
                } else {
                    requestTimeout = GlobalTimer.getTimer().newTimeout(
                            requestTimer, REQUEST_TIMEOUT, TimeUnit.SECONDS);
                }
            }
        }

        private synchronized void response() {
            logEvent("response context: " + activeContext.hashCode());
            if (!destroyed) {
                ServletResponse response = activeContext.getResponse();

                JsonObjectBuilder result = Json.createObjectBuilder();
                result.add("success", true);
                result.add("data", JsonConverter.arrayToJson(positions.values()));
                positions.clear();

                try {
                    response.getWriter().println(result.build().toString());
                } catch (IOException error) {
                    Log.warning(error);
                }

                activeContext.complete();
                activeContext = null;

                sessionTimeout = GlobalTimer.getTimer().newTimeout(
                        sessionTimer, SESSION_TIMEOUT, TimeUnit.SECONDS);
            }
        }

    }

    private static final Map<Long, AsyncSession> ASYNC_SESSIONS = new HashMap<>();

    public static void sessionRefreshUser(long userId) {
        synchronized (ASYNC_SESSIONS) {
            ASYNC_SESSIONS.remove(userId);
        }
    }

    public static void sessionRefreshDevice(long deviceId) {
        synchronized (ASYNC_SESSIONS) {
            Iterator<Entry<Long, AsyncSession>> iterator = ASYNC_SESSIONS.entrySet().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getValue().hasDevice(deviceId)) {
                    iterator.remove();
                }
            }
        }
    }

    private void async(final AsyncContext context, long userId) {

        context.setTimeout(ASYNC_TIMEOUT);
        HttpServletRequest req = (HttpServletRequest) context.getRequest();

        synchronized (ASYNC_SESSIONS) {

            if (Boolean.parseBoolean(req.getParameter("first")) || !ASYNC_SESSIONS.containsKey(userId)) {
                Collection<Long> devices = Context.getPermissionsManager().allowedDevices(userId);
                ASYNC_SESSIONS.put(userId, new AsyncSession(userId, devices));
            }

            ASYNC_SESSIONS.get(userId).request(context);
        }
    }

}
