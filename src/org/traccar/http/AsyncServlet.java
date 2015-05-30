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
package org.traccar.http;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.traccar.Context;
import org.traccar.GlobalTimer;
import org.traccar.database.DataCache;
import org.traccar.helper.Log;
import org.traccar.model.Position;
import org.traccar.model.User;

public class AsyncServlet extends HttpServlet {

    private static final long ASYNC_TIMEOUT = 120000;
    
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        async(req.startAsync());
    }
    
    public class AsyncSession {
        
        private static final boolean DEBUG_ASYNC = false;
        
        private static final long SESSION_TIMEOUT = 30;
        private static final long REQUEST_TIMEOUT = 10;
        
        private boolean destroyed;
        private final long userId;
        private final Collection<Long> devices;
        private Timeout sessionTimeout;
        private Timeout requestTimeout;
        private final Map<Long, Position> positions = new HashMap<Long, Position>();
        private AsyncContext activeContext;
        
        private void logEvent(String message) {
            if (DEBUG_ASYNC) {
                Log.debug("AsyncSession: " + this.hashCode() + " destroyed: " + destroyed + " " + message);
            }
        }
        
        public AsyncSession(long userId, Collection<Long> devices) {
            logEvent("create userId: " + userId + " devices: " + devices.size());
            this.userId = userId;
            this.devices = devices;

            Collection<Position> initialPositions = Context.getDataCache().getInitialState(devices);
            for (Position position : initialPositions) {
                positions.put(position.getDeviceId(), position);
            }
            
            Context.getDataCache().addListener(devices, dataListener);
        }
        
        private final DataCache.DataCacheListener dataListener = new DataCache.DataCacheListener() {
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
                    Context.getDataCache().removeListener(devices, dataListener);
                    synchronized (asyncSessions) {
                        asyncSessions.remove(userId);
                    }
                    destroyed = true;
                }
            }
        };
                
        private final TimerTask requestTimer = new TimerTask() {
            @Override
            public void run(Timeout tmt) throws Exception {
                synchronized (AsyncSession.this) {
                    logEvent("requestTimeout");
                    if (!destroyed) {
                        if (activeContext != null) {
                            response();
                        }
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
    
    private final Map<Long, AsyncSession> asyncSessions = new HashMap<Long, AsyncSession>();
    
    private void async(final AsyncContext context) {
        
        context.setTimeout(ASYNC_TIMEOUT);
        HttpServletRequest req = (HttpServletRequest) context.getRequest();
        User user = (User) req.getSession().getAttribute(MainServlet.USER_KEY);
        
        synchronized (asyncSessions) {
            
            if (!asyncSessions.containsKey(user.getId())) {
                Collection<Long> devices = Context.getPermissionsManager().allowedDevices(user.getId());
                asyncSessions.put(user.getId(), new AsyncSession(user.getId(), devices));
            }
            
            asyncSessions.get(user.getId()).request(context);
        }
    }

}
