/*
 * Copyright 2012 - 2013 Anton Tananaev (anton.tananaev@gmail.com)
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
import java.util.logging.LogRecord;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
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
 * Server Manager
 */
public class ServerManager {

    private final List<TrackerServer> serverList = new LinkedList<TrackerServer>();

    public void addTrackerServer(TrackerServer trackerServer) {
        serverList.add(trackerServer);
    }

    private boolean loggerEnabled;

    public boolean isLoggerEnabled() {
        return loggerEnabled;
    }

    private DataManager dataManager;

    public DataManager getDataManager() {
        return dataManager;
    }

    private ReverseGeocoder reverseGeocoder;

    public ReverseGeocoder getReverseGeocoder() {
        return reverseGeocoder;
    }

    private WebServer webServer;

    public WebServer getWebServer() {
        return webServer;
    }

    private Properties properties;

    public Properties getProperties() {
        return  properties;
    }

    /**
     * Initialize
     */
    public void init(String[] arguments)
            throws IOException, ClassNotFoundException, SQLException {

        // Load properties
        properties = new Properties();
        if (arguments.length > 0) {
            properties.loadFromXML(new FileInputStream(arguments[0]));
        }

        dataManager = new DatabaseDataManager(properties);

        initLogger(properties);
        initGeocoder(properties);

        initXexunServer("xexun");
        initGps103Server("gps103");
        initTk103Server("tk103");
        initGl100Server("gl100");
        initGl200Server("gl200");
        initT55Server("t55");
        initXexun2Server("xexun2");
        initAvl08Server("avl08");
        initEnforaServer("enfora");
        initMeiligaoServer("meiligao");
        initMaxonServer("maxon");
        initST210Server("st210");
        initProgressServer("progress");
        initH02Server("h02");
        initJt600Server("jt600");
        initEv603Server("ev603");
        initV680Server("v680");
        initPt502Server("pt502");
        initTr20Server("tr20");
        initNavisServer("navis");
        initMeitrackServer("meitrack");
        initSkypatrolServer("skypatrol");
        initGt02Server("gt02");
        initGt06Server("gt06");
        initMegastekServer("megastek");
        initNavigilServer("navigil");
        initGpsGateServer("gpsgate");
        initTeltonikaServer("teltonika");
        initMta6Server("mta6");
        initMta6CanServer("mta6can");
        initTlt2hServer("tlt2h");

        // Initialize web server
        if (Boolean.valueOf(properties.getProperty("http.enable"))) {
            webServer = new WebServer(properties);
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
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    @Override
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

                Log.getLogger().addHandler(file);
            }
        }
    }

    private void initGeocoder(Properties properties) throws IOException {
        if (Boolean.parseBoolean(properties.getProperty("geocoder.enable"))) {
            reverseGeocoder = new GoogleReverseGeocoder();
        }
    }

    private boolean isProtocolEnabled(Properties properties, String protocol) {
        String enabled = properties.getProperty(protocol + ".enable");
        if (enabled != null) {
            return Boolean.valueOf(enabled);
        }
        return false;
    }

    private void initXexunServer(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new XexunFrameDecoder());
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new XexunProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initGps103Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) ';' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gps103ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initTk103Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) ')' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tk103ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initGl100Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) 0x0 };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gl100ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initGl200Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '$' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Gl200ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initT55Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new T55ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initXexun2Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\n' }; // tracker bug \n\r
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Xexun2ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initAvl08Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Avl08ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initEnforaServer(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 0, 2, -2, 2));
                    pipeline.addLast("objectDecoder", new EnforaProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initMeiligaoServer(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new MeiligaoFrameDecoder());
                    pipeline.addLast("objectDecoder", new MeiligaoProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initMaxonServer(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new MaxonProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initST210Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new ST210ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initProgressServer(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            TrackerServer server = new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1024, 2, 2, 4, 0));
                    pipeline.addLast("objectDecoder", new ProgressProtocolDecoder(ServerManager.this));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initH02Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '#' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new H02ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initJt600Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new Jt600FrameDecoder());
                    pipeline.addLast("objectDecoder", new Jt600ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initEv603Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) ';' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new Ev603ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initV680Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '#', (byte) '#' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("objectDecoder", new V680ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initPt502Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Pt502ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initTr20Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tr20ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initNavisServer(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            TrackerServer server = new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(4 * 1024, 12, 2, 2, 0));
                    pipeline.addLast("objectDecoder", new NavisProtocolDecoder(ServerManager.this));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initMeitrackServer(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new MeitrackProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initSkypatrolServer(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ConnectionlessBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("objectDecoder", new SkypatrolProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initGt02Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(256, 2, 1, 2, 0));
                    pipeline.addLast("objectDecoder", new Gt02ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initGt06Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(256, 2, 1, 2, 0));
                    pipeline.addLast("objectDecoder", new Gt06ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initMegastekServer(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new MegastekProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initNavigilServer(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            TrackerServer server = new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new NavigilFrameDecoder());
                    pipeline.addLast("objectDecoder", new NavigilProtocolDecoder(ServerManager.this));
                }
            };
            server.setEndianness(ByteOrder.LITTLE_ENDIAN);
            serverList.add(server);
        }
    }

    private void initGpsGateServer(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '\r', (byte) '\n' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new GpsGateProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initTeltonikaServer(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("frameDecoder", new TeltonikaFrameDecoder());
                    pipeline.addLast("objectDecoder", new TeltonikaProtocolDecoder(ServerManager.this));
                }
            });
        }
    }
    
    private void initMta6Server(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("httpDecoder", new HttpRequestDecoder());
                    pipeline.addLast("httpEncoder", new HttpResponseEncoder());
                    pipeline.addLast("objectDecoder", new Mta6ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

    private void initMta6CanServer(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("httpDecoder", new HttpRequestDecoder());
                    pipeline.addLast("httpEncoder", new HttpResponseEncoder());
                    pipeline.addLast("objectDecoder", new Mta6ProtocolDecoder(ServerManager.this));
                }
            });
        }
    }
    
    private void initTlt2hServer(String protocol) throws SQLException {
        if (isProtocolEnabled(properties, protocol)) {
            serverList.add(new TrackerServer(this, new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    byte delimiter[] = { (byte) '#', (byte) '#' };
                    pipeline.addLast("frameDecoder",
                            new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                    pipeline.addLast("stringDecoder", new StringDecoder());
                    pipeline.addLast("stringEncoder", new StringEncoder());
                    pipeline.addLast("objectDecoder", new Tlt2hProtocolDecoder(ServerManager.this));
                }
            });
        }
    }

}
