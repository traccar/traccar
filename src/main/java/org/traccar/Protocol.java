/*
 * Copyright 2018 - 2025 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.Channel;
import org.traccar.model.Command;

import java.net.SocketAddress;
import java.util.Collection;

public interface Protocol {

    String getName();

    Collection<TrackerConnector> getConnectorList();

    Collection<String> getSupportedDataCommands();

    void sendDataCommand(Channel channel, SocketAddress remoteAddress, Command command);

    Collection<String> getSupportedTextCommands();

    void sendTextCommand(String destAddress, Command command) throws Exception;

    Collection<String> getSupportedPushCommands();

}
