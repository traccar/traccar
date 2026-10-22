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
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ServerManager implements LifecycleObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerManager.class);

    private final Injector injector;
    private final Config config;
    private final Map<String, Class<? extends BaseProtocol>> protocolClasses = new HashMap<>();
    private final Map<String, BaseProtocol> protocols = new ConcurrentHashMap<>();
    private final Map<TrackerConnector, Integer> runningConnectors = new IdentityHashMap<>();

    @Inject
    public ServerManager(
            Injector injector, Config config) throws IOException, URISyntaxException, ReflectiveOperationException {
        this.injector = injector;
        this.config = config;
        for (Class<?> protocolClass : ClassScanner.findSubclasses(BaseProtocol.class, "org.traccar.protocol")) {
            protocolClasses.put(
                    BaseProtocol.nameFromClass(protocolClass), protocolClass.asSubclass(BaseProtocol.class));
        }
    }

    public BaseProtocol getProtocol(String name) {
        return protocols.get(name);
    }

    @Override
    public synchronized void start() throws Exception {
        Set<String> enabledProtocols = null;
        if (config.hasKey(Keys.PROTOCOLS_ENABLE)) {
            enabledProtocols = new HashSet<>(Arrays.asList(config.getString(Keys.PROTOCOLS_ENABLE).split("[, ]")));
        }
        for (String protocol : protocolClasses.keySet()) {
            int port = config.getInteger(Keys.PROTOCOL_PORT.withPrefix(protocol));
            if (port > 0 && (enabledProtocols == null || enabledProtocols.contains(protocol))) {
                apply(new ProtocolSettings(protocol, port, Transport.ALL));
            }
        }
    }

    public synchronized void apply(ProtocolSettings settings) throws Exception {
        BaseProtocol protocol = protocols.computeIfAbsent(
                settings.getProtocol(), name -> injector.getInstance(protocolClasses.get(name)));
        for (TrackerConnector connector : protocol.getConnectorList()) {
            boolean enabled = settings.getTransport() == Transport.ALL
                    || settings.getTransport() == (connector.isDatagram() ? Transport.UDP : Transport.TCP);
            Integer runningPort = runningConnectors.get(connector);
            if (runningPort != null && (!enabled || runningPort != settings.getPort())) {
                connector.stop();
                runningConnectors.remove(connector);
            }
            if (enabled && !runningConnectors.containsKey(connector)) {
                try {
                    connector.start(settings.getPort());
                    runningConnectors.put(connector, settings.getPort());
                } catch (BindException e) {
                    LOGGER.warn("Port {} disabled due to conflict", settings.getPort(), e);
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
