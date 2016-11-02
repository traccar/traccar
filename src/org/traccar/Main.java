/*
 * Copyright 2012 - 2015 Anton Tananaev (anton@traccar.org)
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
package org.traccar;

import org.traccar.helper.Log;

import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Locale;

public final class Main {

    private static final long CLEAN_PERIOD = 24 * 60 * 60 * 1000;

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ENGLISH);

        Context.init(args);
        Log.info("Starting server...");

        Context.getServerManager().start();
        if (Context.getWebServer() != null) {
            Context.getWebServer().start();
        }

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    Context.getDataManager().clearPositionsHistory();
                } catch (SQLException error) {
                    Log.warning(error);
                }
            }
        }, 0, CLEAN_PERIOD);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Log.info("Shutting down server...");

                if (Context.getWebServer() != null) {
                    Context.getWebServer().stop();
                }
                Context.getServerManager().stop();
            }
        });
    }

}
