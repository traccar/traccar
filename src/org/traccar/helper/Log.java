/*
 * Copyright 2012 - 2018 Anton Tananaev (anton@traccar.org)
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

import org.traccar.Config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class Log {

    private Log() {
    }

    private static final String STACK_PACKAGE = "org.traccar";
    private static final int STACK_LIMIT = 3;

    private static class RollingFileHandler extends Handler {

        private String name;
        private String suffix;
        private Writer writer;

        RollingFileHandler(String name) {
            this.name = name;
        }

        @Override
        public synchronized void publish(LogRecord record) {
            if (isLoggable(record)) {
                try {
                    String suffix = new SimpleDateFormat("yyyyMMdd").format(new Date(record.getMillis()));
                    if (writer != null && !suffix.equals(this.suffix)) {
                        writer.close();
                        writer = null;
                        new File(name).renameTo(new File(name + "." + this.suffix));
                    }
                    if (writer == null) {
                        this.suffix = suffix;
                        writer = new BufferedWriter(new FileWriter(name, true));
                    }
                    writer.write(getFormatter().format(record));
                    writer.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void flush() {
            if (writer != null) {
                try {
                    writer.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void close() throws SecurityException {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    public static class LogFormatter extends Formatter {

        private boolean fullStackTraces;

        LogFormatter(boolean fullStackTraces) {
            this.fullStackTraces = fullStackTraces;
        }

        private static String formatLevel(Level level) {
            switch (level.getName()) {
                case "FINEST":
                    return "TRACE";
                case "FINER":
                case "FINE":
                    return "DEBUG";
                case "INFO":
                    return "INFO";
                case "WARNING":
                    return "WARN";
                case "SEVERE":
                default:
                    return "ERROR";
            }
        }

        @Override
        public String format(LogRecord record) {
            StringBuilder message = new StringBuilder();

            if (record.getMessage() != null) {
                message.append(record.getMessage());
            }

            if (record.getThrown() != null) {
                if (message.length() > 0) {
                    message.append(" - ");
                }
                if (fullStackTraces) {
                    StringWriter stringWriter = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(stringWriter);
                    record.getThrown().printStackTrace(printWriter);
                    message.append(System.lineSeparator()).append(stringWriter.toString());
                } else {
                    message.append(exceptionStack(record.getThrown()));
                }
            }

            return String.format("%1$tF %1$tT %2$5s: %3$s%n",
                    new Date(record.getMillis()), formatLevel(record.getLevel()), message.toString());
        }

    }

    public static void setupLogger(Config config) {

        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        Handler handler;
        if (config.getBoolean("logger.console")) {
            handler = new ConsoleHandler();
        } else {
            handler = new RollingFileHandler(config.getString("logger.file"));
        }

        handler.setFormatter(new LogFormatter(config.getBoolean("logger.fullStackTraces")));

        Level level = Level.parse(config.getString("logger.level").toUpperCase());
        rootLogger.setLevel(level);
        handler.setLevel(level);

        rootLogger.addHandler(handler);
    }

    public static String exceptionStack(Throwable exception) {
        StringBuilder s = new StringBuilder();
        String exceptionMsg = exception.getMessage();
        if (exceptionMsg != null) {
            s.append(exceptionMsg);
            s.append(" - ");
        }
        s.append(exception.getClass().getSimpleName());
        StackTraceElement[] stack = exception.getStackTrace();

        if (stack.length > 0) {
            int count = STACK_LIMIT;
            boolean first = true;
            boolean skip = false;
            String file = "";
            s.append(" (");
            for (StackTraceElement element : stack) {
                if (count > 0 && element.getClassName().startsWith(STACK_PACKAGE)) {
                    if (!first) {
                        s.append(" < ");
                    } else {
                        first = false;
                    }

                    if (skip) {
                        s.append("... < ");
                        skip = false;
                    }

                    if (file.equals(element.getFileName())) {
                        s.append("*");
                    } else {
                        file = element.getFileName();
                        s.append(file.substring(0, file.length() - 5)); // remove ".java"
                        count -= 1;
                    }
                    s.append(":").append(element.getLineNumber());
                } else {
                    skip = true;
                }
            }
            if (skip) {
                if (!first) {
                    s.append(" < ");
                }
                s.append("...");
            }
            s.append(")");
        }
        return s.toString();
    }

}
