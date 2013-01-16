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

import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.traccar.model.DataManager;

/**
 * Base class for protocol decoders
 */
public abstract class BaseProtocolDecoder extends OneToOneDecoder {

    private ServerManager serverManager;
    private DataManager dataManager;

    public final void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public final DataManager getDataManager() {
        return dataManager;
    }

    public final void setServerManager(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    public final ServerManager getServerManager() {
        return serverManager;
    }

    public BaseProtocolDecoder() {
    }

    public BaseProtocolDecoder(ServerManager serverManager) {
        this.serverManager = serverManager;
        dataManager = serverManager.getDataManager();
    }

}
