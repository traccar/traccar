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

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;

public class ServerManager {

    private final List<TrackerServer> serverList = new LinkedList<>();

    public ServerManager() throws Exception {

        List<String> names = new LinkedList<>();
        String packageName = "org.traccar.protocol";
        String packagePath = packageName.replace('.', '/');
        URL packageUrl = Thread.currentThread().getContextClassLoader().getResource(packagePath);

        if (packageUrl.getProtocol().equals("jar")) {
            String jarFileName = URLDecoder.decode(packageUrl.getFile(), "UTF-8");
            try (JarFile jf = new JarFile(jarFileName.substring(5, jarFileName.indexOf("!")))) {
                Enumeration<JarEntry> jarEntries = jf.entries();
                while (jarEntries.hasMoreElements()) {
                    String entryName = jarEntries.nextElement().getName();
                    if (entryName.startsWith(packagePath) && entryName.length() > packagePath.length() + 5) {
                        names.add(entryName.substring(packagePath.length() + 1, entryName.lastIndexOf('.')));
                    }
                }
            }
        } else {
            File folder = new File(new URI(packageUrl.toString()));
            File[] files = folder.listFiles();
            if (files != null) {
                for (File actual: files) {
                    String entryName = actual.getName();
                    names.add(entryName.substring(0, entryName.lastIndexOf('.')));
                }
            }
        }

        for (String name : names) {
            Class protocolClass = Class.forName(packageName + '.' + name);
            if (BaseProtocol.class.isAssignableFrom(protocolClass)) {
                initProtocolServer((BaseProtocol) protocolClass.newInstance());
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
        return Context.getConfig().hasKey(protocol + ".port");
    }

    private void initProtocolDetector() {
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

    private void initProtocolServer(final Protocol protocol) {
        if (isProtocolEnabled(protocol.getName())) {
            protocol.initTrackerServers(serverList);
        }
    }

}
