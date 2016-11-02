/*
 * Copyright 2015 - 2016 Anton Tananaev (anton@traccar.org)
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

import org.h2.server.web.ConnectionInfo;
import org.h2.server.web.WebServlet;
import org.traccar.Context;
import org.traccar.helper.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ConsoleServlet extends WebServlet {

    @Override
    public void init() {
        super.init();

        try {
            Field field = WebServlet.class.getDeclaredField("server");
            field.setAccessible(true);
            org.h2.server.web.WebServer server = (org.h2.server.web.WebServer) field.get(this);

            ConnectionInfo connectionInfo = new ConnectionInfo("Traccar|"
                    + Context.getConfig().getString("database.driver") + "|"
                    + Context.getConfig().getString("database.url") + "|"
                    + Context.getConfig().getString("database.user"));

            Method method;

            method = org.h2.server.web.WebServer.class.getDeclaredMethod("updateSetting", ConnectionInfo.class);
            method.setAccessible(true);
            method.invoke(server, connectionInfo);

            method = org.h2.server.web.WebServer.class.getDeclaredMethod("setAllowOthers", boolean.class);
            method.setAccessible(true);
            method.invoke(server, true);

        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Log.warning(e);
        }
    }

}
