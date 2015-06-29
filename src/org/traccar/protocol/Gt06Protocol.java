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
package org.traccar.protocol;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelPipeline;
import org.traccar.BaseProtocol;
import org.traccar.TrackerServer;
import org.traccar.database.ActiveDevice;
import org.traccar.helper.Crc;
import org.traccar.command.CommandType;
import org.traccar.command.NoParameterCommand;
import org.traccar.command.CommandTemplate;

import java.util.List;
import java.util.Map;

public class Gt06Protocol extends BaseProtocol {

    public Gt06Protocol() {
        super("gt06");
    }

    @Override
    protected void initCommandsTemplates(Map<CommandType, CommandTemplate> templates) {
        templates.put(CommandType.STOP_ENGINE, new Gt06CommandTemplate("DYD#"));
        templates.put(CommandType.RESUME_ENGINE, new Gt06CommandTemplate("HFYD#"));
    }

    @Override
    public void initTrackerServers(List<TrackerServer> serverList) {
        serverList.add(new TrackerServer(new ServerBootstrap(), this.getName()) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                pipeline.addLast("frameDecoder", new Gt06FrameDecoder());
                pipeline.addLast("objectDecoder", new Gt06ProtocolDecoder(Gt06Protocol.this));
            }
        });
    }

    class Gt06CommandTemplate implements CommandTemplate<NoParameterCommand> {

        private String commandContent;

        public Gt06CommandTemplate(String commandContent) {
            this.commandContent = commandContent;
        }

        @Override
        public Object applyTo(ActiveDevice activeDevice, NoParameterCommand command) {
            int serverFlagBit = 0x0;
            int commandLength = serverFlagBit + commandContent.length();
            int packetLength =  0x80 /*Protocol Number */ + commandContent.length() /* Information Content */ + 2 /*Information Serial Number */+  2 /*Error Check */;

            int informationSerialNumber = 1;


            ChannelBuffer response = ChannelBuffers.directBuffer(10);
            response.writeBytes(new byte[]{0x78, 0x78}); // Start Bit
            response.writeByte(packetLength); // Packet Length
            response.writeByte(0x80); // Protocol Number

            // Information Content
            response.writeByte(commandLength); // Length of command
            response.writeByte(serverFlagBit); // Server Flag Bit
            response.writeBytes(commandContent.getBytes()); // Command Content
            response.writeBytes(new byte[]{0x00, 0x02}); // Language

            response.writeShort(informationSerialNumber); // Information Serial Number

            int crc = Crc.crc16Ccitt(response.toByteBuffer(2, response.writerIndex()));
            response.writeShort(crc); // Error Check

            response.writeBytes(new byte[] {0x0D, 0x0A}); // Stop Bit

            return response;
        }
    }

}
