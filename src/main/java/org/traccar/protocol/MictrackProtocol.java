/*
 * Copyright 2019 - 2026 Anton Tananaev (anton@traccar.org)
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

import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.traccar.BaseProtocol;
import org.traccar.PipelineBuilder;
import org.traccar.TrackerServer;
import org.traccar.config.Config;
import org.traccar.model.Command;

import jakarta.inject.Inject;

/**
 * Mictrack GPS tracker protocol — all device families on a single port.
 *
 * <p>Frame format is detected from the first byte by {@link MictrackFrameDecoder}:
 *
 * <ul>
 *   <li><b>{@code *HQ,...#}</b> — HQ protocol (MT532 and compatible)
 *       <ul>
 *         <li>MT532 — compact asset/vehicle tracker; V1/V5/V6 position, V4 heartbeat,
 *             4-byte active-low vehicle status bitmask (SOS, overspeed, ignition, geofence, etc.)</li>
 *       </ul>
 *   </li>
 *   <li><b>{@code #IMEI#MODEL#...##}</b> — MT700-style protocol; model field in header selects alarm mapping
 *       <ul>
 *         <li>MT700 — portable battery-powered asset tracker; GPRMC + WiFi location;
 *             alarms: TOWED, SHAKE, DEF (light-sensor removal), BLP (backup battery)</li>
 *         <li>MT700W — MT700 variant with enhanced WiFi scanning; same alarm set</li>
 *         <li>MT600 — vehicle tracker with ACC input; GPRMC + WiFi location;
 *             alarms: DEF (power cut), HT (temperature), SOS, OVERSPEED, OS/RS (geofence), BLP, CLP</li>
 *         <li>MT530 — compact vehicle tracker; same frame and alarm set as MT600</li>
 *       </ul>
 *   </li>
 *   <li><b>Newline-terminated</b> — legacy MT format ({@code MT;N;IMEI;RN;...} or {@code IMEI$GPRMC...})</li>
 * </ul>
 */
public class MictrackProtocol extends BaseProtocol {

    @Inject
    public MictrackProtocol(Config config) {
        setSupportedDataCommands(
                Command.TYPE_CUSTOM,
                Command.TYPE_REBOOT_DEVICE,
                Command.TYPE_POSITION_PERIODIC,
                Command.TYPE_MODE_DEEP_SLEEP,
                Command.TYPE_SET_CONNECTION,
                Command.TYPE_GET_DEVICE_STATUS,
                Command.TYPE_ENGINE_STOP,
                Command.TYPE_ENGINE_RESUME,
                Command.TYPE_ALARM_ARM,
                Command.TYPE_ALARM_DISARM);
        addServer(new TrackerServer(config, getName(), false) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new MictrackFrameDecoder());
                pipeline.addLast(new StringEncoder());
                pipeline.addLast(new StringDecoder());
                pipeline.addLast(new MictrackProtocolEncoder(MictrackProtocol.this));
                pipeline.addLast(new MictrackProtocolDecoder(MictrackProtocol.this));
            }
        });
        addServer(new TrackerServer(config, getName(), true) {
            @Override
            protected void addProtocolHandlers(PipelineBuilder pipeline, Config config) {
                pipeline.addLast(new StringEncoder());
                pipeline.addLast(new StringDecoder());
                pipeline.addLast(new MictrackProtocolEncoder(MictrackProtocol.this));
                pipeline.addLast(new MictrackProtocolDecoder(MictrackProtocol.this));
            }
        });
    }

}
