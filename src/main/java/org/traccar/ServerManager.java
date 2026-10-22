/*
 * Copyright 2012 - 2026 Anton Tananaev (anton@traccar.org)
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
import org.traccar.model.ProtocolSettings;
import org.traccar.model.ProtocolSettings.Transport;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ServerManager implements LifecycleObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerManager.class);

    private final Config config;
    private final Map<String, BaseProtocol> protocols = new ConcurrentHashMap<>();
    private final Map<TrackerConnector, Integer> runningConnectors = new IdentityHashMap<>();

    @Inject
    public ServerManager(
            Injector injector, Config config) throws IOException, URISyntaxException, ReflectiveOperationException {
        this.config = config;
        Set<String> enabledProtocols = null;
        if (config.hasKey(Keys.PROTOCOLS_ENABLE)) {
            enabledProtocols = new HashSet<>(Arrays.asList(config.getString(Keys.PROTOCOLS_ENABLE).split("[, ]")));
        }
        for (Class<?> protocolClass : ClassScanner.findSubclasses(BaseProtocol.class, "org.traccar.protocol")) {
            String protocolName = BaseProtocol.nameFromClass(protocolClass);
            if (enabledProtocols == null || enabledProtocols.contains(protocolName)) {
                if (config.getInteger(Keys.PROTOCOL_PORT.withPrefix(protocolName)) > 0) {
                    BaseProtocol protocol = (BaseProtocol) injector.getInstance(protocolClass);
                    protocols.put(protocol.getName(), protocol);
                }
            }
        }
    }

    public BaseProtocol getProtocol(String name) {
        return protocols.get(name);
    }

    @Override
    public synchronized void start() throws Exception {
        for (BaseProtocol protocol : protocols.values()) {
            int port = config.getInteger(Keys.PROTOCOL_PORT.withPrefix(protocol.getName()));
            Transport transport = port > 0 ? Transport.ALL : Transport.DISABLED;
            apply(new ProtocolSettings(protocol.getName(), port, transport));
        }
    }

    public synchronized void apply(ProtocolSettings settings) throws Exception {
        BaseProtocol protocol = protocols.get(settings.protocol());
        for (TrackerConnector connector : protocol.getConnectorList()) {
            boolean enabled = settings.transport() == Transport.ALL
                    || settings.transport() == (connector.isDatagram() ? Transport.UDP : Transport.TCP);
            Integer runningPort = runningConnectors.get(connector);
            if (runningPort != null && (!enabled || runningPort != settings.port())) {
                connector.stop();
                runningConnectors.remove(connector);
            }
            if (enabled && !runningConnectors.containsKey(connector)) {
                try {
                    connector.start(settings.port());
                    runningConnectors.put(connector, settings.port());
                } catch (BindException e) {
                    LOGGER.warn("Port {} disabled due to conflict", settings.port(), e);
                } catch (ConnectException e) {
                    LOGGER.warn("Connection failed", e);
                }
            }
        }
    }

    @Override
    public synchronized void stop() throws Exception {
        for (BaseProtocol protocol : protocols.values()) {
            for (TrackerConnector connector : protocol.getConnectorList()) {
                connector.stop();
                runningConnectors.remove(connector);
            }
        }
    }

}
