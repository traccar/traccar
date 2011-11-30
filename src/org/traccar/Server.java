/*
 * Copyright 2010 Anton Tananaev (anton@tananaev.com)
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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.traccar.helper.NamedParameterStatement;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.traccar.protocol.xexun.XexunFrameDecoder;
import org.traccar.protocol.xexun.XexunProtocolDecoder;
import org.traccar.protocol.gps103.Gps103ProtocolDecoder;
import org.traccar.protocol.tk103.Tk103ProtocolDecoder;
import org.traccar.protocol.gl200.Gl200ProtocolDecoder;
import org.traccar.protocol.t55.T55ProtocolDecoder;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.traccar.helper.AdvancedConnection;
import org.traccar.protocol.gl100.Gl100ProtocolDecoder;
import org.traccar.protocol.xexun2.Xexun2ProtocolDecoder;

/**
 * Server
 */
public class Server implements DataManager {

    /**
     * Server list
     */
    private List serverList;

    private boolean loggerEnable;

    public Server() {
        serverList = new LinkedList();
        loggerEnable = false;
    }
    
    public boolean isLoggerEnabled() {
        return loggerEnable;
    }

    /**
     * Init
     */
    public void init(String[] arguments)
            throws IOException, ClassNotFoundException, SQLException {

        // Load properties
        Properties properties = new Properties();
        if (arguments.length > 0) {
            properties.loadFromXML(new FileInputStream(arguments[0]));
        }

        initDatabase(properties);
        initLogger(properties);

        initXexunServer(properties);
        initGps103Server(properties);
        initTk103Server(properties);
        initGl100Server(properties);
        initGl200Server(properties);
        initT55Server(properties);
        initXexun2Server(properties);
    }

    /**
     * Database statements
     */
    private NamedParameterStatement selectDevice;
    private NamedParameterStatement insertPosition;

    /**
     * Init database
     */
    private void initDatabase(Properties properties)
            throws ClassNotFoundException, SQLException {

        // Load driver
        String driver = properties.getProperty("database.driver");
        if (driver != null) {
            Class.forName(driver);
        }

        // Connect database
        String url = properties.getProperty("database.url");
        String user = properties.getProperty("database.user");
        String password = properties.getProperty("database.password");
        AdvancedConnection connection = new AdvancedConnection(url, user, password);

        String query = properties.getProperty("database.selectDevice");
        selectDevice = new NamedParameterStatement(connection, query);

        query = properties.getProperty("database.insertPosition");
        insertPosition = new NamedParameterStatement(connection, query);
    }

    /**
     * Devices
     */
    private Map devices;
    private Calendar devicesLastUpdate;
    private Long devicesListRefreshDelay = new Long(5 * 60 * 1000);

    public synchronized List getDevices() throws SQLException {

        List deviceList = new LinkedList();

        ResultSet result = selectDevice.executeQuery();
        while (result.next()) {
            Device device = new Device();
            device.setId(result.getLong("id"));
            device.setImei(result.getString("imei"));
            deviceList.add(device);
        }

        return deviceList;
    }
    
    public Device getDeviceByImei(String imei) throws SQLException {
        
        if ((devices == null) || (Calendar.getInstance().getTimeInMillis() - devicesLastUpdate.getTimeInMillis() > devicesListRefreshDelay)) {
            devices = new HashMap();
            List deviceList = getDevices();
            for (Object device: deviceList) {
                devices.put(((Device) device).getImei(), device);
            }
            devicesLastUpdate = Calendar.getInstance();
        }

        return (Device) devices.get(imei);
    }

    public synchronized void setPosition(Position position) throws SQLException {

        insertPosition.setLong("device_id", position.getDeviceId());
        insertPosition.setTimestamp("time", position.getTime());
        insertPosition.setBoolean("valid", position.getValid());
        insertPosition.setDouble("altitude", position.getAltitude());
        insertPosition.setDouble("latitude", position.getLatitude());
        insertPosition.setDouble("longitude", position.getLongitude());
        insertPosition.setDouble("speed", position.getSpeed());
        insertPosition.setDouble("course", position.getCourse());
        insertPosition.setDouble("power", position.getPower());

        insertPosition.executeUpdate();
    }
    
    /**
     * Init logger
     */
    public void initLogger(Properties properties) throws IOException {

        loggerEnable = Boolean.valueOf(properties.getProperty("logger.enable"));

        if (loggerEnable) {

            Logger logger = Logger.getLogger("logger");
            String fileName = properties.getProperty("logger.file");
            if (fileName != null) {

                FileHandler file = new FileHandler(fileName, true);

                file.setFormatter(new Formatter() {
                    private final String LINE_SEPARATOR =
                            System.getProperty("line.separator", "\n");
                    
                    private final DateFormat dateFormat =
                            new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

                    public String format(LogRecord record) {
                        String line = dateFormat.format(new Date(record.getMillis()));
                        line += " - ";
                        line += formatMessage(record);
                        line += LINE_SEPARATOR;
                        return line;
                    }
                });

                logger.setLevel(Level.ALL);
                logger.addHandler(file);
            }
        }
    }

    /**
     * Open channel handler
     */
    protected class OpenChannelHandler extends SimpleChannelHandler {

        private TrackerServer server;

        public OpenChannelHandler(TrackerServer server) {
            this.server = server;
        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
            server.getChannelGroup().add(e.getChannel());
        }
    }

    /**
     * Xexun pipeline factory
     */
    protected class XexunPipelineFactory implements ChannelPipelineFactory {

        private TrackerServer server;
        private Server serverCreator;
        private Integer resetDelay;
        
        public XexunPipelineFactory(
                TrackerServer server, Server serverCreator, Integer resetDelay) {
            this.server = server;
            this.serverCreator = serverCreator;
            this.resetDelay = resetDelay;
        }
        
        public ChannelPipeline getPipeline() {
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("openHandler", new OpenChannelHandler(server));
            if (serverCreator.isLoggerEnabled()) {
                pipeline.addLast("logger", new LoggingHandler("logger"));
            }
            pipeline.addLast("frameDecoder", new XexunFrameDecoder());
            pipeline.addLast("stringDecoder", new StringDecoder());
            pipeline.addLast("objectDecoder", new XexunProtocolDecoder(serverCreator, resetDelay));
            pipeline.addLast("handler", new TrackerEventHandler(serverCreator));            
            return pipeline;
        }
    }
    
    /**
     * Init Xexun server
     */
    public void initXexunServer(Properties properties) throws SQLException {

        boolean enable = Boolean.valueOf(properties.getProperty("xexun.enable"));
        if (enable) {

            TrackerServer server = new TrackerServer(
                    Integer.valueOf(properties.getProperty("xexun.port")));

            String resetDelay = properties.getProperty("xexun.resetDelay");
            server.setPipelineFactory(new XexunPipelineFactory(
                    server, this, (resetDelay == null) ? 0 : Integer.valueOf(resetDelay)));

            serverList.add(server);
        }
    }

    /**
     * Gps103 pipeline factory
     */
    protected class Gps103PipelineFactory implements ChannelPipelineFactory {

        private TrackerServer server;
        private Server serverCreator;
        private Integer resetDelay;
        
        public Gps103PipelineFactory(
                TrackerServer server, Server serverCreator, Integer resetDelay) {
            this.server = server;
            this.serverCreator = serverCreator;
            this.resetDelay = resetDelay;
        }
        
        public ChannelPipeline getPipeline() {
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("openHandler", new OpenChannelHandler(server));
            if (serverCreator.isLoggerEnabled()) {
                pipeline.addLast("logger", new LoggingHandler("logger"));
            }
            byte delimiter[] = { (byte) ';' };
            pipeline.addLast("frameDecoder",
                    new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
            pipeline.addLast("stringDecoder", new StringDecoder());
            pipeline.addLast("stringEncoder", new StringEncoder());
            pipeline.addLast("objectDecoder", new Gps103ProtocolDecoder(serverCreator, resetDelay));
            pipeline.addLast("handler", new TrackerEventHandler(serverCreator));            
            return pipeline;
        }
    }
    
    /**
     * Init Gps103 server
     */
    public void initGps103Server(Properties properties) throws SQLException {

        boolean enable = Boolean.valueOf(properties.getProperty("gps103.enable"));
        if (enable) {

            TrackerServer server = new TrackerServer(
                    Integer.valueOf(properties.getProperty("gps103.port")));

            String resetDelay = properties.getProperty("gps103.resetDelay");
            server.setPipelineFactory(new Gps103PipelineFactory(
                    server, this, (resetDelay == null) ? 0 : Integer.valueOf(resetDelay)));

            serverList.add(server);
        }
    }

    /**
     * Tk103 pipeline factory
     */
    protected class Tk103PipelineFactory implements ChannelPipelineFactory {

        private TrackerServer server;
        private Server serverCreator;
        private Integer resetDelay;
        
        public Tk103PipelineFactory(
                TrackerServer server, Server serverCreator, Integer resetDelay) {
            this.server = server;
            this.serverCreator = serverCreator;
            this.resetDelay = resetDelay;
        }
        
        public ChannelPipeline getPipeline() {
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("openHandler", new OpenChannelHandler(server));
            if (serverCreator.isLoggerEnabled()) {
                pipeline.addLast("logger", new LoggingHandler("logger"));
            }
            byte delimiter[] = { (byte) ')' };
            pipeline.addLast("frameDecoder",
                    new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
            pipeline.addLast("stringDecoder", new StringDecoder());
            pipeline.addLast("stringEncoder", new StringEncoder());
            pipeline.addLast("objectDecoder", new Tk103ProtocolDecoder(serverCreator, resetDelay));
            pipeline.addLast("handler", new TrackerEventHandler(serverCreator));            
            return pipeline;
        }
    }    
    
    /**
     * Init Tk103 server
     */
    public void initTk103Server(Properties properties) throws SQLException {

        boolean enable = Boolean.valueOf(properties.getProperty("tk103.enable"));
        if (enable) {

            TrackerServer server = new TrackerServer(
                    Integer.valueOf(properties.getProperty("tk103.port")));

            String resetDelay = properties.getProperty("tk103.resetDelay");
            server.setPipelineFactory(new Tk103PipelineFactory(
                    server, this, (resetDelay == null) ? 0 : Integer.valueOf(resetDelay)));

            serverList.add(server);
        }
    }

    /**
     * Gl100 pipeline factory
     */
    protected class Gl100PipelineFactory implements ChannelPipelineFactory {

        private TrackerServer server;
        private Server serverCreator;
        private Integer resetDelay;
        
        public Gl100PipelineFactory(
                TrackerServer server, Server serverCreator, Integer resetDelay) {
            this.server = server;
            this.serverCreator = serverCreator;
            this.resetDelay = resetDelay;
        }
        
        public ChannelPipeline getPipeline() {
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("openHandler", new OpenChannelHandler(server));
            if (serverCreator.isLoggerEnabled()) {
                pipeline.addLast("logger", new LoggingHandler("logger"));
            }
            byte delimiter[] = { (byte) 0x0 };
            pipeline.addLast("frameDecoder",
                    new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
            pipeline.addLast("stringDecoder", new StringDecoder());
            pipeline.addLast("stringEncoder", new StringEncoder());
            pipeline.addLast("objectDecoder", new Gl100ProtocolDecoder(serverCreator, resetDelay));
            pipeline.addLast("handler", new TrackerEventHandler(serverCreator));
            return pipeline;
        }
    }    
    
    /**
     * Init Gl100 server
     */
    public void initGl100Server(Properties properties) throws SQLException {

        boolean enable = Boolean.valueOf(properties.getProperty("gl100.enable"));
        if (enable) {

            TrackerServer server = new TrackerServer(
                    Integer.valueOf(properties.getProperty("gl100.port")));

            String resetDelay = properties.getProperty("gl100.resetDelay");
            server.setPipelineFactory(new Gl100PipelineFactory(
                    server, this, (resetDelay == null) ? 0 : Integer.valueOf(resetDelay)));

            serverList.add(server);
        }
    }

    
    /**
     * Gl200 pipeline factory
     */
    protected class Gl200PipelineFactory implements ChannelPipelineFactory {

        private TrackerServer server;
        private Server serverCreator;
        private Integer resetDelay;
        
        public Gl200PipelineFactory(
                TrackerServer server, Server serverCreator, Integer resetDelay) {
            this.server = server;
            this.serverCreator = serverCreator;
            this.resetDelay = resetDelay;
        }
        
        public ChannelPipeline getPipeline() {
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("openHandler", new OpenChannelHandler(server));
            if (serverCreator.isLoggerEnabled()) {
                pipeline.addLast("logger", new LoggingHandler("logger"));
            }
            byte delimiter[] = { (byte) '$' };
            pipeline.addLast("frameDecoder",
                    new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
            pipeline.addLast("stringDecoder", new StringDecoder());
            pipeline.addLast("stringEncoder", new StringEncoder());
            pipeline.addLast("objectDecoder", new Gl200ProtocolDecoder(serverCreator, resetDelay));
            pipeline.addLast("handler", new TrackerEventHandler(serverCreator));
            return pipeline;
        }
    }    
    
    /**
     * Init Gl200 server
     */
    public void initGl200Server(Properties properties) throws SQLException {

        boolean enable = Boolean.valueOf(properties.getProperty("gl200.enable"));
        if (enable) {

            TrackerServer server = new TrackerServer(
                    Integer.valueOf(properties.getProperty("gl200.port")));

            String resetDelay = properties.getProperty("gl200.resetDelay");
            server.setPipelineFactory(new Gl200PipelineFactory(
                    server, this, (resetDelay == null) ? 0 : Integer.valueOf(resetDelay)));

            serverList.add(server);
        }
    }
    
    /**
     * T55 pipeline factory
     */
    protected class T55PipelineFactory implements ChannelPipelineFactory {

        private TrackerServer server;
        private Server serverCreator;
        private Integer resetDelay;
        
        public T55PipelineFactory(
                TrackerServer server, Server serverCreator, Integer resetDelay) {
            this.server = server;
            this.serverCreator = serverCreator;
            this.resetDelay = resetDelay;
        }
        
        public ChannelPipeline getPipeline() {
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("openHandler", new OpenChannelHandler(server));
            if (serverCreator.isLoggerEnabled()) {
                pipeline.addLast("logger", new LoggingHandler("logger"));
            }
            byte delimiter[] = { (byte) '\r', (byte) '\n' };
            pipeline.addLast("frameDecoder",
                    new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
            pipeline.addLast("stringDecoder", new StringDecoder());
            pipeline.addLast("stringEncoder", new StringEncoder());
            pipeline.addLast("objectDecoder", new T55ProtocolDecoder(serverCreator, resetDelay));
            pipeline.addLast("handler", new TrackerEventHandler(serverCreator));
            return pipeline;
        }
    }    
    
    /**
     * Init T55 server
     */
    public void initT55Server(Properties properties) throws SQLException {

        boolean enable = Boolean.valueOf(properties.getProperty("t55.enable"));
        if (enable) {

            TrackerServer server = new TrackerServer(
                    Integer.valueOf(properties.getProperty("t55.port")));

            String resetDelay = properties.getProperty("t55.resetDelay");
            server.setPipelineFactory(new T55PipelineFactory(
                    server, this, (resetDelay == null) ? 0 : Integer.valueOf(resetDelay)));

            serverList.add(server);
        }
    }
    
    /**
     * Xexun 2 pipeline factory
     */
    protected class Xexun2PipelineFactory implements ChannelPipelineFactory {

        private TrackerServer server;
        private Server serverCreator;
        private Integer resetDelay;
        
        public Xexun2PipelineFactory(
                TrackerServer server, Server serverCreator, Integer resetDelay) {
            this.server = server;
            this.serverCreator = serverCreator;
            this.resetDelay = resetDelay;
        }
        
        public ChannelPipeline getPipeline() {
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("openHandler", new OpenChannelHandler(server));
            if (serverCreator.isLoggerEnabled()) {
                pipeline.addLast("logger", new LoggingHandler("logger"));
            }
            byte delimiter[] = { (byte) '\n' }; // tracker bug \n\r
            pipeline.addLast("frameDecoder",
                    new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
            pipeline.addLast("stringDecoder", new StringDecoder());
            pipeline.addLast("objectDecoder", new Xexun2ProtocolDecoder(serverCreator, resetDelay));
            pipeline.addLast("handler", new TrackerEventHandler(serverCreator));            
            return pipeline;
        }
    }
    
    /**
     * Init Xexun 2 server
     */
    public void initXexun2Server(Properties properties) throws SQLException {

        boolean enable = Boolean.valueOf(properties.getProperty("xexun2.enable"));
        if (enable) {

            TrackerServer server = new TrackerServer(
                    Integer.valueOf(properties.getProperty("xexun2.port")));

            String resetDelay = properties.getProperty("xexun2.resetDelay");
            server.setPipelineFactory(new Xexun2PipelineFactory(
                    server, this, (resetDelay == null) ? 0 : Integer.valueOf(resetDelay)));

            serverList.add(server);
        }
    }

    /**
     * Start
     */
    public void start() {
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
    }

    /**
     * Destroy
     */
    public void destroy() {
        serverList.clear();
    }

}
