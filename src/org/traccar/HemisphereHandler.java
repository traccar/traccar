/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.channel.Channel;
import org.traccar.model.Position;

import java.net.SocketAddress;

public class HemisphereHandler extends ExtendedObjectDecoder {

    private int latitudeFactor;
    private int longitudeFactor;

    public HemisphereHandler() {
        String latitudeHemisphere = Context.getConfig().getString("location.latitudeHemisphere");
        if (latitudeHemisphere != null) {
            if (latitudeHemisphere.equalsIgnoreCase("N")) {
                latitudeFactor = 1;
            } else if (latitudeHemisphere.equalsIgnoreCase("S")) {
                latitudeFactor = -1;
            }
        }
        String longitudeHemisphere = Context.getConfig().getString("location.longitudeHemisphere");
        if (longitudeHemisphere != null) {
            if (longitudeHemisphere.equalsIgnoreCase("E")) {
                longitudeFactor = 1;
            } else if (longitudeHemisphere.equalsIgnoreCase("W")) {
                longitudeFactor = -1;
            }
        }
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        if (msg instanceof Position) {
            Position position = (Position) msg;
            if (latitudeFactor != 0) {
                position.setLatitude(Math.abs(position.getLatitude()) * latitudeFactor);
            }
            if (longitudeFactor != 0) {
                position.setLongitude(Math.abs(position.getLongitude()) * longitudeFactor);
            }
        }

        return msg;
    }

}
