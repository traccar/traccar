/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.util.Timer;
import java.util.TimerTask;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.traccar.model.DataManager;

/**
 * Base class for protocol decoders
 */
public abstract class GenericProtocolDecoder extends OneToOneDecoder {

    /**
     * Data manager
     */
    private DataManager dataManager;

    /**
     * Set data manager
     */
    public final void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Return data manager
     */
    public final DataManager getDataManager() {
        return dataManager;
    }

    /**
     * Reset connection delay
     */
    private Integer resetDelay;

    /**
     * Set reset connection delay
     */
    public final void setResetDelay(Integer resetDelay) {
        this.resetDelay = resetDelay;
    }

    /**
     * Default constructor
     */
    public GenericProtocolDecoder() {
    }

    /**
     * Initialize
     */
    public GenericProtocolDecoder(DataManager dataManager, Integer resetDelay) {
        setDataManager(dataManager);
        setResetDelay(resetDelay);
    }

    /**
     * Disconnect channel
     */
    private class DisconnectTask extends TimerTask {
        private Channel channel;

        public DisconnectTask(Channel channel) {
            this.channel = channel;
        }

        public void run() {
            channel.disconnect();
        }
    }

    /**
     * Handle connect event
     */
    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {
        super.handleUpstream(ctx, evt);

        if (evt instanceof ChannelStateEvent) {
            ChannelStateEvent event = (ChannelStateEvent) evt;

            if (event.getState() == ChannelState.CONNECTED && event.getValue() != null && resetDelay != 0) {
                new Timer().schedule(new GenericProtocolDecoder.DisconnectTask(evt.getChannel()), resetDelay);
            }
        }
    }

}
