/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.helper;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logger
 */
public class Log {
    
    private static String TRACCAR_LOGGER_NAME = "traccar";

    /**
     * Return global logger
     */
    public static Logger getLogger() {
        return Logger.getLogger(TRACCAR_LOGGER_NAME);
    }
    
    private static void write(Level level, String msg) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        if (stack != null && stack.length > 3) {
            getLogger().logp(level, stack[3].getClassName(), stack[3].getMethodName(), msg);
        } else {
            getLogger().log(level, msg);
        }
    }

    public static void severe(String msg) {
        write(Level.SEVERE, msg);
    }

    public static void warning(String msg) {
        write(Level.WARNING, msg);
    }

    public static void info(String msg) {
        write(Level.INFO, msg);
    }

    public static void fine(String msg) {
        write(Level.FINE, msg);
    }

}
