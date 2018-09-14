/*
 * Copyright 2015 - 2018 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.geolocation.GeolocationProvider;
import org.traccar.model.Position;

@ChannelHandler.Sharable
public class GeolocationHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeolocationHandler.class);

    private final GeolocationProvider geolocationProvider;
    private final boolean processInvalidPositions;

    public GeolocationHandler(GeolocationProvider geolocationProvider, boolean processInvalidPositions) {
        this.geolocationProvider = geolocationProvider;
        this.processInvalidPositions = processInvalidPositions;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object message) throws Exception {
        if (message instanceof Position) {
            final Position position = (Position) message;
            if ((position.getOutdated() || processInvalidPositions && !position.getValid())
                    && position.getNetwork() != null) {
                Context.getStatisticsManager().registerGeolocationRequest();

                geolocationProvider.getLocation(position.getNetwork(),
                        new GeolocationProvider.LocationProviderCallback() {
                    @Override
                    public void onSuccess(double latitude, double longitude, double accuracy) {
                        position.set(Position.KEY_APPROXIMATE, true);
                        position.setValid(true);
                        position.setFixTime(position.getDeviceTime());
                        position.setLatitude(latitude);
                        position.setLongitude(longitude);
                        position.setAccuracy(accuracy);
                        position.setAltitude(0);
                        position.setSpeed(0);
                        position.setCourse(0);
                        position.set(Position.KEY_RSSI, 0);
                        ctx.fireChannelRead(position);
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        LOGGER.warn("Geolocation network error", e);
                        ctx.fireChannelRead(position);
                    }
                });
            } else {
                ctx.fireChannelRead(position);
            }
        } else {
            ctx.fireChannelRead(message);
        }
    }

}
