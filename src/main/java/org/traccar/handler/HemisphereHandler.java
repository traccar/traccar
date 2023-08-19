/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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
package org.traccar.handler;

import io.netty.channel.ChannelHandler;
import org.traccar.BaseDataHandler;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Position;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
public class HemisphereHandler extends BaseDataHandler {

    private int latitudeFactor;
    private int longitudeFactor;

    @Inject
    public HemisphereHandler(Config config) {
        String latitudeHemisphere = config.getString(Keys.LOCATION_LATITUDE_HEMISPHERE);
        if (latitudeHemisphere != null) {
            if (latitudeHemisphere.equalsIgnoreCase("N")) {
                latitudeFactor = 1;
            } else if (latitudeHemisphere.equalsIgnoreCase("S")) {
                latitudeFactor = -1;
            }
        }
        String longitudeHemisphere = config.getString(Keys.LOCATION_LONGITUDE_HEMISPHERE);
        if (longitudeHemisphere != null) {
            if (longitudeHemisphere.equalsIgnoreCase("E")) {
                longitudeFactor = 1;
            } else if (longitudeHemisphere.equalsIgnoreCase("W")) {
                longitudeFactor = -1;
            }
        }
    }

    @Override
    protected Position handlePosition(Position position) {
        if (latitudeFactor != 0) {
            position.setLatitude(Math.abs(position.getLatitude()) * latitudeFactor);
        }
        if (longitudeFactor != 0) {
            position.setLongitude(Math.abs(position.getLongitude()) * longitudeFactor);
        }
        return position;
    }

}
