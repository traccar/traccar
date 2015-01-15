/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.traccar.database.DataManager;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.helper.Log;
import org.traccar.model.Position;

import java.util.List;

public class DetectorHandler extends SimpleChannelHandler {

    private List<TrackerServer> serverList;

    DetectorHandler(List<TrackerServer> serverList) {
        this.serverList = serverList;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

        if (e.getMessage() instanceof ChannelBuffer) {
            ChannelBuffer buf = (ChannelBuffer) e.getMessage();

            for (TrackerServer server : serverList) {
                try {
                    ChannelPipeline pipeline = server.getPipelineFactory().getPipeline();

                    if (pipeline.get("stringDecoder") != null) {

                        /*ChannelBuffer tmp = buf.duplicate();
                        FrameDecoder frameDecoder = (FrameDecoder) pipeline.get("frameDecoder");
                        if (frameDecoder != null) {
                            tmp = frameDecoder.
                        }*/



                    }
                } catch(Exception error) {
                    Log.warning(error);
                }
            }
        }
    }

}
