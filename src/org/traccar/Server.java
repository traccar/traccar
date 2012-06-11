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
package org.traccar;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.traccar.helper.Log;
import org.traccar.http.WebServer;
import org.traccar.model.DataManager;
import org.traccar.model.DatabaseDataManager;
import org.traccar.protocol.*;

/**
 * Server
 */
public class Server {

    /**
     * Server list
     */
    private List serverList;

    private boolean loggerEnabled;

    public Server() {
        serverList = new LinkedList();
        loggerEnabled = false;
    }

    public boolean isLoggerEnabled() {
        return loggerEnabled;
    }

    private DataManager dataManager;

    private WebServer webServer;

    /**
     * Initialize
     */
    public void init(String[] arguments)
            throws IOException, ClassNotFoundException, SQLException {

        // Load properties
        Properties properties = new Properties();
        if (arguments.length > 0) {
            properties.loadFromXML(new FileInputStream(arguments[0]));
        }

        dataManager = new DatabaseDataManager(properties);

        initLogger(properties);

        initXexunServer(properties);
        initGps103Server(properties);
        initTk103Server(properties);
        initGl100Server(properties);
        initGl200Server(properties);
        initT55Server(properties);
        initXexun2Server(properties);
        initAvl08Server(properties);
        initEnforaServer(properties);
        initMeiligaoServer(properties);

        // Initialize web server
        if (Boolean.valueOf(properties.getProperty("http.enable"))) {
            Integer port = Integer.valueOf(properties.getProperty("http.port", "8082"));
            webServer = new WebServer(port, dataManager);
        }
    }

    /**
     * Start
     */
    public void start() {
        if (webServer != null) {
            webServer.start();
        }
        for (Object server: serverList) {
            ((TrackerServer) server).start();
        }
    }

    /**
     * Stop
     */
    public void stop() {
        for (Object server: serverList) {
            ((TrackerServer) server).stop();
        }
        if (webServer != null) {
            webServer.stop();
        }
    }

    /**
     * Destroy
     */
    public void destroy() {
        serverList.clear();
    }

    /**
     * Initialize logger
     */
    public void initLogger(Properties properties) throws IOException {

        loggerEnabled = Boolean.valueOf(properties.getProperty("logger.enable"));

        if (loggerEnabled) {

            String fileName = properties.getProperty("logger.file");
            if (fileName != null) {

                FileHandler file = new FileHandler(fileName, true);

                // Simple formatter
                file.setFormatter(new Formatter() {
                    private final String LINE_SEPARATOR =
                            System.getProperty("line.separator", "\n");

                    private final DateFormat dateFormat =
                            new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

                    public String format(LogRecord record) {
                        String line = dateFormat.format(new Date(record.getMillis()));
                        line += " " + record.getLevel().getName() + ": ";
                        line += formatMessage(record);
                        line += LINE_SEPARATOR;
                        return line;
                    }
                });

                // NOTE: Console logger level will remain INFO
                Log.getLogger().setLevel(Level.ALL);
                Log.getLogger().addHandler(file);
            }
        }
    }

    private boolean isProtocolEnabled(Properties properties, String protocol) {
        String enabled = properties.getProperty(protocol + ".enable");
        if (enabled != null) {
            return Boolean.valueOf(enabled);
        }
        return false;
    }

    private Integer getProtocolPort(Properties properties, String protocol) {
        String port = properties.getProperty(protocol + ".port");
        if (port != null) {
            return Integer.valueOf(port);
        }
        return 5000; // Magic number
    }

    private Integer getProtocolResetDelay(Properties properties, String protocol) {
        String resetDelay = properties.getProperty(protocol + ".resetDelay");
        if (resetDelay != null) {
            return Integer.valueOf(resetDelay);
        }
        return 0;
    }

    /**
     * Init Xexun server
     */
    public void initXexunServer(Properties properties) throws SQLException {

        String protocol = "xexun";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(getProtocolPort(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled()) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new XexunFrameDecoder());
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new XexunProtocolDecoder(getDataManager(), resetDelay));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Gps103 server
     */
    public void initGps103Server(Properties properties) throws SQLException {

        String protocol = "gps103";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(getProtocolPort(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled()) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) ';' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gps103ProtocolDecoder(getDataManager(), resetDelay));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Tk103 server
     */
    public void initTk103Server(Properties properties) throws SQLException {

        String protocol = "tk103";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(getProtocolPort(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled()) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) ')' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tk103ProtocolDecoder(getDataManager(), resetDelay));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Gl100 server
     */
    public void initGl100Server(Properties properties) throws SQLException {

        String protocol = "gl100";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(getProtocolPort(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled()) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) 0x0 };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gl100ProtocolDecoder(getDataManager(), resetDelay));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Gl200 server
     */
    public void initGl200Server(Properties properties) throws SQLException {

        String protocol = "gl200";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(getProtocolPort(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled()) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '$' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gl200ProtocolDecoder(getDataManager(), resetDelay));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init T55 server
     */
    public void initT55Server(Properties properties) throws SQLException {

        String protocol = "t55";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(getProtocolPort(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled()) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new T55ProtocolDecoder(getDataManager(), resetDelay));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Xexun 2 server
     */
    public void initXexun2Server(Properties properties) throws SQLException {

        String protocol = "xexun2";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(getProtocolPort(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled()) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\n' }; // tracker bug \n\r
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Xexun2ProtocolDecoder(getDataManager(), resetDelay));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init AVL-08 server
     */
    public void initAvl08Server(Properties properties) throws SQLException {

        String protocol = "avl08";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(getProtocolPort(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled()) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Avl08ProtocolDecoder(getDataManager(), resetDelay));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Enfora server
     */
    public void initEnforaServer(Properties properties) throws SQLException {

        String protocol = "enfora";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(getProtocolPort(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled()) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 0, 2, -2, 2));
                    pipeline.addLast("objectDecoder", new EnforaProtocolDecoder(getDataManager(), resetDelay));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Meiligao server
     */
    public void initMeiligaoServer(Properties properties) throws SQLException {

        String protocol = "meiligao";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(getProtocolPort(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled()) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 2, 2, -4, 4));
                    pipeline.addLast("objectDecoder", new MeiligaoProtocolDecoder(getDataManager(), resetDelay));
                }
            });

            serverList.add(server);
        }
    }

}
