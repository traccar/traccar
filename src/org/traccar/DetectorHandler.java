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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.traccar.helper.Log;

import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.List;

public class DetectorHandler extends SimpleChannelHandler {

    private final List<TrackerServer> serverList;

    private boolean showFailed;

    DetectorHandler(List<TrackerServer> serverList) {
        this.serverList = serverList;
    }

    public void checkPipeline(String protocol, ChannelPipeline pipeline, ChannelBuffer buf) throws Exception {
        Object tmp = buf.duplicate();

        // Frame decoder
        FrameDecoder frameDecoder = (FrameDecoder) pipeline.get("frameDecoder");
        if (frameDecoder != null) {
            try {
                Method method = frameDecoder.getClass().getDeclaredMethod(
                        "decode", ChannelHandlerContext.class, Channel.class, ChannelBuffer.class);
                method.setAccessible(true);
                tmp = method.invoke(frameDecoder, null, null, tmp);
            } catch (NoSuchMethodException error) {
                Method method = frameDecoder.getClass().getSuperclass().getDeclaredMethod(
                        "decode", ChannelHandlerContext.class, Channel.class, ChannelBuffer.class);
                method.setAccessible(true);
                tmp = method.invoke(frameDecoder, null, null, tmp);
            }
        }

        // String decoder
        if (pipeline.get("stringDecoder") != null) {
            StringDecoder stringDecoder = new StringDecoder();
            if (tmp != null) {
                try {
                    Method method = stringDecoder.getClass().getDeclaredMethod(
                            "decode", ChannelHandlerContext.class, Channel.class, Object.class);
                    method.setAccessible(true);
                    tmp = method.invoke(stringDecoder, null, null, tmp);
                } catch (NoSuchMethodException error) {
                    Method method = stringDecoder.getClass().getSuperclass().getDeclaredMethod(
                            "decode", ChannelHandlerContext.class, Channel.class, Object.class);
                    method.setAccessible(true);
                    tmp = method.invoke(stringDecoder, null, null, tmp);
                }
            }
        }

        // Protocol decoder
        BaseProtocolDecoder protocolDecoder = (BaseProtocolDecoder) pipeline.get("objectDecoder");
        if (tmp != null) {
            try {
                Method method = protocolDecoder.getClass().getDeclaredMethod(
                        "decode", ChannelHandlerContext.class, Channel.class, SocketAddress.class, Object.class);
                method.setAccessible(true);
                tmp = method.invoke(protocolDecoder, null, null, null, tmp);
            } catch (NoSuchMethodException error) {
                Method method = protocolDecoder.getClass().getSuperclass().getDeclaredMethod(
                        "decode", ChannelHandlerContext.class, Channel.class, SocketAddress.class, Object.class);
                method.setAccessible(true);
                tmp = method.invoke(protocolDecoder, null, null, null, tmp);
            }
        }

        if (tmp != null) {
            Log.info("Protocol " + protocol + " possible match");
        } else if (showFailed) {
            Log.info("Protocol " + protocol + " no match");
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

        if (e.getMessage() instanceof ChannelBuffer) {
            ChannelBuffer buf = (ChannelBuffer) e.getMessage();

            for (TrackerServer server : serverList) {
                try {
                    if (!server.getProtocol().equals("detector")) {
                        checkPipeline(server.getProtocol(), server.getPipelineFactory().getPipeline(), buf);
                    }
                } catch (Exception error) {
                    if (showFailed) {
                        Log.info("Protocol " + server.getProtocol() + " error");
                    }
                }
            }
        }
    }

}
