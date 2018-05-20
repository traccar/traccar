/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
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

import java.util.List;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.traccar.BaseProtocol;
import org.traccar.CharacterDelimiterFrameDecoder;
import org.traccar.TrackerServer;
import org.traccar.model.Command;

/**
 *
 * @author Samson
 */
public class StrongTowerProtocol extends BaseProtocol {

    public StrongTowerProtocol() {
        super("strongtower");
        setSupportedCommands(
                Command.TYPE_ALARM_ARM,
                Command.TYPE_ALARM_BATTERY,
                Command.TYPE_ALARM_CLOCK,
                Command.TYPE_ALARM_DISARM,
                Command.TYPE_ALARM_FALL,
                Command.TYPE_ALARM_GEOFENCE,
                Command.TYPE_ALARM_REMOVE,
                Command.TYPE_ALARM_SOS,
                Command.TYPE_ALARM_SPEED,
                Command.TYPE_ALARM_VIBRATION,
                Command.TYPE_CONFIGURATION,
                Command.TYPE_CUSTOM,
                Command.TYPE_ENGINE_RESUME,
                Command.TYPE_ENGINE_STOP,
                Command.TYPE_FIRMWARE_UPDATE,
                Command.TYPE_GET_DEVICE_STATUS,
                Command.TYPE_GET_MODEM_STATUS,
                Command.TYPE_GET_VERSION,
                Command.TYPE_IDENTIFICATION,
                Command.TYPE_MODE_DEEP_SLEEP,
                Command.TYPE_MODE_POWER_SAVING,
                Command.TYPE_OUTPUT_CONTROL,
                Command.TYPE_POSITION_PERIODIC,
                Command.TYPE_POSITION_SINGLE,
                Command.TYPE_POSITION_STOP,
                Command.TYPE_REBOOT_DEVICE,
                Command.TYPE_REQUEST_PHOTO,
                Command.TYPE_SEND_SMS,
                Command.TYPE_SEND_USSD,
                Command.TYPE_SET_AGPS,
                Command.TYPE_SET_CONNECTION,
                Command.TYPE_SET_INDICATOR,
                Command.TYPE_SET_ODOMETER,
                Command.TYPE_SET_PHONEBOOK,
                Command.TYPE_SET_TIMEZONE,
                Command.TYPE_SILENCE_TIME,
                Command.TYPE_SOS_NUMBER,
                Command.TYPE_VOICE_MESSAGE,
                Command.TYPE_VOICE_MONITORING
        );
    }

    @Override
    public void initTrackerServers(List<TrackerServer> serverList) {
        serverList.add(new TrackerServer(new ServerBootstrap(), getName()) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(2048, "\r\n", "\n"));
                //would be nice to have a JsonObjectEncoder of netty 4/5
                pipeline.addLast("stringEncoder", new StringEncoder());
                pipeline.addLast("stringDecoder", new StringDecoder());
                pipeline.addLast("objectEncoder", new StrongTowerProtocolEncoder());
                pipeline.addLast("objectDecoder", new StrongTowerProtocolDecoder(StrongTowerProtocol.this));
            }
        });
    }

}
