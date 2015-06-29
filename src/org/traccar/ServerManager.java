/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;

public class ServerManager {

    private final List<TrackerServer> serverList = new LinkedList<TrackerServer>();

    public void init() throws Exception {

        ClassLoader cl = ServerManager.class.getClassLoader();
        String pack = "org/traccar/protocol";
        String dottedPackage = pack.replaceAll("[/]", ".");

        BufferedReader reader = new BufferedReader(new InputStreamReader(cl.getResourceAsStream(pack)));
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.endsWith(".class")) {
                Class protocolClass = Class.forName(dottedPackage + "." + line.substring(0, line.lastIndexOf('.')));
                if (BaseProtocol.class.isAssignableFrom(protocolClass)) {
                    initProtocolServer((BaseProtocol) protocolClass.newInstance());
                }
            }
        }

        initProtocolDetector();
    }

    public void start() {
        for (Object server: serverList) {
            ((TrackerServer) server).start();
        }
    }

    public void stop() {
        for (Object server: serverList) {
            ((TrackerServer) server).stop();
        }

        // Release resources
        GlobalChannelFactory.release();
        GlobalTimer.release();
    }

    private boolean isProtocolEnabled(String protocol) {
        return Context.getProps().containsKey(protocol + ".port");
    }

    private void initProtocolDetector() throws SQLException {
        String protocol = "detector";
        if (isProtocolEnabled(protocol)) {
            serverList.add(new TrackerServer(new ServerBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("detectorHandler", new DetectorHandler(serverList));
                }
            });
            serverList.add(new TrackerServer(new ConnectionlessBootstrap(), protocol) {
                @Override
                protected void addSpecificHandlers(ChannelPipeline pipeline) {
                    pipeline.addLast("detectorHandler", new DetectorHandler(serverList));
                }
            });
        }
    }

    private void initProtocolServer(final Protocol protocol) throws SQLException {
        if (isProtocolEnabled(protocol.getName())) {
            protocol.initTrackerServers(serverList);
        }
    }

}
