/*
 * Copyright 2012 - 2024 Anton Tananaev (anton@traccar.org)
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

import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class Log {

    private Log() {
    }

    private static final String STACK_PACKAGE = "org.traccar";
    private static final int STACK_LIMIT = 4;

    private static class RollingFileHandler extends Handler {

        private final String name;
        private String suffix;
        private Writer writer;
        private final boolean rotate;
        private final String template;

        RollingFileHandler(String name, boolean rotate, String rotateInterval) {
            this.name = name;
            this.rotate = rotate;
            this.template = rotateInterval.equalsIgnoreCase("HOUR") ? "yyyyMMddHH" : "yyyyMMdd";
        }

        @Override
        public synchronized void publish(LogRecord record) {
            if (isLoggable(record)) {
                try {
                    String suffix = "";
                    if (rotate) {
                        suffix = new SimpleDateFormat(template).format(new Date(record.getMillis()));
                        if (writer != null && !suffix.equals(this.suffix)) {
                            writer.close();
                            writer = null;
                            if (!new File(name).renameTo(new File(name + "." + this.suffix))) {
                                throw new RuntimeException("Log file renaming failed");
                            }
                        }
                    }
                    if (writer == null) {
                        this.suffix = suffix;
                        writer = new BufferedWriter(
                                new OutputStreamWriter(new FileOutputStream(name, true), StandardCharsets.UTF_8));
                    }
                    writer.write(getFormatter().format(record));
                    writer.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public synchronized void flush() {
            if (writer != null) {
                try {
                    writer.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public synchronized void close() throws SecurityException {
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

        private final boolean fullStackTraces;

        LogFormatter(boolean fullStackTraces) {
            this.fullStackTraces = fullStackTraces;
        }

        private static String formatLevel(Level level) {
            return switch (level.getName()) {
                case "FINEST" -> "TRACE";
                case "FINER", "FINE", "CONFIG" -> "DEBUG";
                case "INFO" -> "INFO";
                case "WARNING" -> "WARN";
                default -> "ERROR";
            };
        }

        @Override
        public String format(LogRecord record) {
            StringBuilder message = new StringBuilder();

            if (record.getMessage() != null) {
                message.append(record.getMessage());
            }

            if (record.getThrown() != null) {
                if (!message.isEmpty()) {
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

    public static void setupDefaultLogger() {
        String path = null;
        URL url = ClassLoader.getSystemClassLoader().getResource(".");
        if (url != null) {
            File jarPath = new File(url.getPath());
            File logsPath = new File(jarPath, "logs");
            if (!logsPath.exists() || !logsPath.isDirectory()) {
                logsPath = jarPath;
            }
            path = new File(logsPath, "tracker-server.log").getPath();
        }
        setupLogger(path == null, path, Level.WARNING.getName(), false, true, "DAY");
    }

    public static void setupLogger(Config config) {
        setupLogger(
                config.getBoolean(Keys.LOGGER_CONSOLE),
                config.getString(Keys.LOGGER_FILE),
                config.getString(Keys.LOGGER_LEVEL),
                config.getBoolean(Keys.LOGGER_FULL_STACK_TRACES),
                config.getBoolean(Keys.LOGGER_ROTATE),
                config.getString(Keys.LOGGER_ROTATE_INTERVAL));
    }

    private static void setupLogger(
            boolean console, String file, String levelString,
            boolean fullStackTraces, boolean rotate, String rotateInterval) {

        Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        Handler handler;
        if (console) {
            handler = new ConsoleHandler();
        } else {
            handler = new RollingFileHandler(file, rotate, rotateInterval);
        }

        handler.setFormatter(new LogFormatter(fullStackTraces));

        Level level = Level.parse(levelString.toUpperCase());
        rootLogger.setLevel(level);
        handler.setLevel(level);
        handler.setFilter(record -> record != null && !record.getLoggerName().startsWith("sun"));

        rootLogger.addHandler(handler);
    }

    public static String exceptionStack(Throwable exception) {
        Throwable cause = exception.getCause();
        while (cause != null && exception != cause) {
            exception = cause;
            cause = cause.getCause();
        }

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
                        s.append(file, 0, file.length() - 5); // remove ".java"
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

    public static long[] getStorageSpace() {
        var stores = new ArrayList<Pair<Long, Long>>();
        for (FileStore store : FileSystems.getDefault().getFileStores()) {
            try {
                long usableSpace = store.getUsableSpace();
                long totalSpace = store.getTotalSpace();
                if (totalSpace > 1_000_000_000) {
                    stores.add(new Pair<>(usableSpace, totalSpace));
                }
            } catch (IOException ignored) {
            }
        }
        return stores.stream()
                .sorted(Comparator.comparingDouble(p -> p.first() / (double) p.second()))
                .flatMap(p -> Stream.of(p.first(), p.second()))
                .mapToLong(Long::longValue)
                .toArray();
    }

}
