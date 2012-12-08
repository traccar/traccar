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
import java.nio.ByteOrder;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.traccar.geocode.GoogleReverseGeocoder;
import org.traccar.geocode.ReverseGeocoder;
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
    private List<TrackerServer> serverList;

    private boolean loggerEnabled;

    public Server() {
        serverList = new LinkedList<TrackerServer>();
        loggerEnabled = false;
    }

    public boolean isLoggerEnabled() {
        return loggerEnabled;
    }

    private DataManager dataManager;

    private WebServer webServer;

    private ReverseGeocoder geocoder;

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
        initGeocoder(properties);

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
        initMaxonServer(properties);
        initST210Server(properties);
        initProgressServer(properties);
        initH02Server(properties);
        initJt600Server(properties);
        initEv603Server(properties);
        initV680Server(properties);
        initPt502Server(properties);
        initTr20Server(properties);
        initNavisServer(properties);
        initMeitrackServer(properties);

        // Initialize web server
        if (Boolean.valueOf(properties.getProperty("http.enable"))) {
            Integer port = Integer.valueOf(properties.getProperty("http.port", "8082"));
            String address = properties.getProperty("http.address");
            if (address != null) {
                webServer = new WebServer(address, port, dataManager);
            } else {
                webServer = new WebServer(port, dataManager);
            }
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
        
        // Release resources
        GlobalChannelFactory.release();
        GlobalTimer.release();
        
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
    private void initLogger(Properties properties) throws IOException {

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
                        StringBuffer line = new StringBuffer();
                        dateFormat.format(new Date(record.getMillis()), line, new FieldPosition(0));
                        line.append(" ");
                        line.append(record.getSourceClassName());
                        line.append(".");
                        line.append(record.getSourceMethodName());
                        line.append(" ");
                        line.append(record.getLevel().getName());
                        line.append(": ");
                        line.append(formatMessage(record));
                        line.append(LINE_SEPARATOR);
                        return line.toString();
                    }
                });

                // NOTE: Console logger level will remain INFO
                Log.getLogger().setLevel(Level.ALL);
                Log.getLogger().addHandler(file);
            }
        }
    }

    private void initGeocoder(Properties properties) throws IOException {
        if (Boolean.parseBoolean(properties.getProperty("geocoder.enable"))) {
            geocoder = new GoogleReverseGeocoder();
        }
    }

    private boolean isProtocolEnabled(Properties properties, String protocol) {
        String enabled = properties.getProperty(protocol + ".enable");
        if (enabled != null) {
            return Boolean.valueOf(enabled);
        }
        return false;
    }

    private String getProtocolInterface(Properties properties, String protocol) {
        return properties.getProperty(protocol + ".address");
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
    private void initXexunServer(Properties properties) throws SQLException {

        String protocol = "xexun";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new XexunFrameDecoder());
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new XexunProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Gps103 server
     */
    private void initGps103Server(Properties properties) throws SQLException {

        String protocol = "gps103";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) ';' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gps103ProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Tk103 server
     */
    private void initTk103Server(Properties properties) throws SQLException {

        String protocol = "tk103";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) ')' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tk103ProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Gl100 server
     */
    private void initGl100Server(Properties properties) throws SQLException {

        String protocol = "gl100";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) 0x0 };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gl100ProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Gl200 server
     */
    private void initGl200Server(Properties properties) throws SQLException {

        String protocol = "gl200";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '$' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gl200ProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init T55 server
     */
    private void initT55Server(Properties properties) throws SQLException {

        String protocol = "t55";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new T55ProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Xexun 2 server
     */
    private void initXexun2Server(Properties properties) throws SQLException {

        String protocol = "xexun2";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\n' }; // tracker bug \n\r
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Xexun2ProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init AVL-08 server
     */
    private void initAvl08Server(Properties properties) throws SQLException {

        String protocol = "avl08";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Avl08ProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Enfora server
     */
    private void initEnforaServer(Properties properties) throws SQLException {

        String protocol = "enfora";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 0, 2, -2, 2));
                    pipeline.addLast("objectDecoder", new EnforaProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Meiligao server
     */
    private void initMeiligaoServer(Properties properties) throws SQLException {

        String protocol = "meiligao";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 2, 2, -4, 4));
                    pipeline.addLast("objectDecoder", new MeiligaoProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    private void initMaxonServer(Properties properties) throws SQLException {
        String protocol = "maxon";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new MaxonProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    private void initST210Server(Properties properties) throws SQLException {
        String protocol = "st210";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new ST210ProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    private void initProgressServer(Properties properties) throws SQLException {
        String protocol = "progress";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 2, 2, 0, 0));
                    pipeline.addLast("objectDecoder", new ProgressProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init H02 server
     */
    private void initH02Server(Properties properties) throws SQLException {

        String protocol = "h02";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '#' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new H02ProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init JT600 server
     */
    private void initJt600Server(Properties properties) throws SQLException {

        String protocol = "jt600";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new Jt600FrameDecoder());
                    pipeline.addLast("objectDecoder", new Jt600ProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }
    
    /**
     * Init EV603 server
     */
    private void initEv603Server(Properties properties) throws SQLException {

        String protocol = "ev603";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) ';' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Ev603ProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }
    
    /**
     * Init V680 server
     */
    private void initV680Server(Properties properties) throws SQLException {

        String protocol = "v680";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '#', (byte) '#' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new V680ProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }
    
    /**
     * Init PT502 server
     */
    private void initPt502Server(Properties properties) throws SQLException {

        String protocol = "pt502";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Pt502ProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init TR20 server
     */
    private void initTr20Server(Properties properties) throws SQLException {

        String protocol = "tr20";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tr20ProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Navis server
     */
    private void initNavisServer(Properties properties) throws SQLException {

        String protocol = "navis";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(4 * 1024, 12, 2, 2, 0));
                    pipeline.addLast("objectDecoder", new NavisProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }

    /**
     * Init Meitrack server
     */
    private void initMeitrackServer(Properties properties) throws SQLException {

        String protocol = "meitrack";
        if (isProtocolEnabled(properties, protocol)) {

            TrackerServer server = new TrackerServer(new ServerBootstrap());
            server.setPort(getProtocolPort(properties, protocol));
            server.setAddress(getProtocolInterface(properties, protocol));
            final Integer resetDelay = getProtocolResetDelay(properties, protocol);

            server.setPipelineFactory(new GenericPipelineFactory(server, dataManager, isLoggerEnabled(), resetDelay, geocoder) {
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new MeitrackProtocolDecoder(getDataManager()));
                }
            });

            serverList.add(server);
        }
    }
}
