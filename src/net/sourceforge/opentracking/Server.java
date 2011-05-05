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
package net.sourceforge.opentracking;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import net.sourceforge.opentracking.helper.NamedParameterStatement;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.logging.LoggingHandler;
import net.sourceforge.opentracking.protocol.xexun.XexunFrameDecoder;
import net.sourceforge.opentracking.protocol.xexun.XexunProtocolDecoder;
import net.sourceforge.opentracking.protocol.gps103.Gps103ProtocolDecoder;
import net.sourceforge.opentracking.protocol.tk103.Tk103ProtocolDecoder;

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
    }

    /**
     * Database connection
     */
    private Connection connection;

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

        if (user != null && password != null) {
            connection = DriverManager.getConnection(url, user, password);
        } else {
            connection = DriverManager.getConnection(url);
        }

        // Init statements
        String selectDeviceQuery = properties.getProperty("database.selectDevice");
        if (selectDeviceQuery != null) {
            selectDevice = new NamedParameterStatement(connection, selectDeviceQuery);
        }

        String insertPositionQuery = properties.getProperty("database.insertPosition");
        if (insertPositionQuery != null) {
            insertPosition = new NamedParameterStatement(connection, insertPositionQuery);
        }
    }

    /**
     * Devices
     */
    private Map devices;

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

        // Init device list
        if (devices == null) {
            devices = new HashMap();

            List deviceList = getDevices();

            for (Object device: deviceList) {
                devices.put(((Device) device).getImei(), device);
            }
        }

        return (Device) devices.get(imei);
    }

    public synchronized void setPosition(Position position) throws SQLException {

        insertPosition.setLong("device_id", position.getDeviceId());
        insertPosition.setTimestamp("time", position.getTime());
        insertPosition.setBoolean("valid", position.getValid());
        insertPosition.setDouble("latitude", position.getLatitude());
        insertPosition.setDouble("longitude", position.getLongitude());
        insertPosition.setDouble("speed", position.getSpeed());
        insertPosition.setDouble("course", position.getCourse());

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

                    public String format(LogRecord record) {
                        return record.getMessage().concat(LINE_SEPARATOR);
                    }
                });

                logger.setLevel(Level.ALL);
                logger.addHandler(file);
            }
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

            if (loggerEnable) {
                server.getPipeline().addLast("logger", new LoggingHandler("logger"));
            }
            server.getPipeline().addLast("frameDecoder", new XexunFrameDecoder());
            server.getPipeline().addLast("stringDecoder", new StringDecoder());
            String resetDelay = properties.getProperty("xexun.resetDelay");
            server.getPipeline().addLast("objectDecoder", new XexunProtocolDecoder(this,
                    (resetDelay == null) ? 0 : Integer.valueOf(resetDelay)));

            server.getPipeline().addLast("handler", new TrackerEventHandler(this));

            serverList.add(server);
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

            if (loggerEnable) {
                server.getPipeline().addLast("logger", new LoggingHandler("logger"));
            }
            byte delimiter[] = { (byte) ';' };
            server.getPipeline().addLast("frameDecoder",
                    new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
            server.getPipeline().addLast("stringDecoder", new StringDecoder());
            server.getPipeline().addLast("stringEncoder", new StringEncoder());
            String resetDelay = properties.getProperty("gps103.resetDelay");
            server.getPipeline().addLast("objectDecoder", new Gps103ProtocolDecoder(this,
                    (resetDelay == null) ? 0 : Integer.valueOf(resetDelay)));

            server.getPipeline().addLast("handler", new TrackerEventHandler(this));

            serverList.add(server);
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

            if (loggerEnable) {
                server.getPipeline().addLast("logger", new LoggingHandler("logger"));
            }
            byte delimiter[] = { (byte) ')' };
            server.getPipeline().addLast("frameDecoder",
                    new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
            server.getPipeline().addLast("stringDecoder", new StringDecoder());
            server.getPipeline().addLast("stringEncoder", new StringEncoder());
            String resetDelay = properties.getProperty("tk103.resetDelay");
            server.getPipeline().addLast("objectDecoder", new Tk103ProtocolDecoder(this,
                    (resetDelay == null) ? 0 : Integer.valueOf(resetDelay)));

            server.getPipeline().addLast("handler", new TrackerEventHandler(this));

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
