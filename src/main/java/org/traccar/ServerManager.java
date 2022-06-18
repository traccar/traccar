/*
 * Copyright 2012 - 2022 Anton Tananaev (anton@traccar.org)
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

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.ClassScanner;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ServerManager implements LifecycleObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerManager.class);

    private final List<TrackerConnector> connectorList = new LinkedList<>();
    private final Map<String, BaseProtocol> protocolList = new ConcurrentHashMap<>();

    @Inject
    public ServerManager(
            Injector injector, Config config) throws IOException, URISyntaxException, ReflectiveOperationException {
        for (Class<?> protocolClass : ClassScanner.findSubclasses(BaseProtocol.class, "org.traccar.protocol")) {
            if (config.hasKey(Keys.PROTOCOL_PORT.withPrefix(BaseProtocol.nameFromClass(protocolClass)))) {
                BaseProtocol protocol = (BaseProtocol) injector.getInstance(protocolClass);
                connectorList.addAll(protocol.getConnectorList());
                protocolList.put(protocol.getName(), protocol);
            }
        }
    }

    public BaseProtocol getProtocol(String name) {
        return protocolList.get(name);
    }

    @Override
    public void start() throws Exception {
        for (TrackerConnector connector: connectorList) {
            try {
                connector.start();
            } catch (BindException e) {
                LOGGER.warn("Port disabled due to conflict", e);
            } catch (ConnectException e) {
                LOGGER.warn("Connection failed", e);
            }
        }
    }

    @Override
    public void stop() throws Exception {
        try {
            for (TrackerConnector connector : connectorList) {
                connector.stop();
            }
        } finally {
            GlobalTimer.release();
        }
    }

}
