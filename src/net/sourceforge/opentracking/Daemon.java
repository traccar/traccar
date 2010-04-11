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

import java.util.List;
import java.util.LinkedList;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.sourceforge.opentracking.helper.NamedParameterStatement;
import org.jboss.netty.handler.codec.string.StringDecoder;
import net.sourceforge.opentracking.protocol.xexun.XexunFrameDecoder;
import net.sourceforge.opentracking.protocol.xexun.XexunProtocolDecoder;

/**
 * Daemon
 */
public class Daemon implements DataManager {

    /**
     * Server list
     */
    private List serverList;

    public Daemon() {
        serverList = new LinkedList();
    }

    /**
     * Init
     */
    public void init(String[] arguments)
            throws IOException, ClassNotFoundException, SQLException {

        // Load properties
        Properties properties = new Properties();
        properties.loadFromXML(new FileInputStream(arguments[0]));
        //properties.loadFromXML(Daemon.class.getResourceAsStream("/configuration.xml"));

        initDatabase(properties);
        initXexunServer(properties);
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

    public synchronized List readDevice() throws SQLException {

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

    public synchronized void writePosition(Position position) throws SQLException {

        insertPosition.setInt("device_id", position.getDeviceId().intValue());
        insertPosition.setTimestamp("time", position.getTime());
        insertPosition.setBoolean("valid", position.getValid());
        insertPosition.setDouble("latitude", position.getLatitude());
        insertPosition.setDouble("longitude", position.getLongitude());
        insertPosition.setDouble("speed", position.getSpeed());
        insertPosition.setDouble("course", position.getCourse());

        insertPosition.executeUpdate();
    }

    /**
     * Init Xexun server
     */
    public void initXexunServer(Properties properties) throws SQLException {

        boolean enable = Boolean.valueOf(properties.getProperty("xexun.enable"));
        if (enable) {

            TrackerServer server = new TrackerServer(
                    Integer.valueOf(properties.getProperty("xexun.port")));

            server.getPipeline().addLast("frameDecoder", new XexunFrameDecoder());
            server.getPipeline().addLast("stringDecoder", new StringDecoder());
            server.getPipeline().addLast("objectDecoder", new XexunProtocolDecoder(this));

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
