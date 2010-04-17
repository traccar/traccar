/*
 * Copyright 2010 Anton Tananaev (anton@tananaev.com)
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
package net.sourceforge.opentracking;

import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.ChannelPipelineCoverage;

/**
 * Tracker message handler
 */
@ChannelPipelineCoverage("all")
public class TrackerEventHandler extends SimpleChannelHandler {

    /**
     * Data manager
     */
    private DataManager dataManager;

    TrackerEventHandler(DataManager newDataManager) {
        super();
        dataManager = newDataManager;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

        //System.out.println("message recieved");

        if (e.getMessage() instanceof Position) {

            // Write position to database
            try {
                dataManager.setPosition((Position) e.getMessage());
            } catch (Exception error) {
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        //System.out.println("error: " + e.getCause().getMessage());
        e.getChannel().close();
    }

}
