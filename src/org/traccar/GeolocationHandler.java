/*
 * Copyright 2015 - 2017 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.traccar.helper.Log;
import org.traccar.geolocation.GeolocationProvider;
import org.traccar.model.Position;

public class GeolocationHandler implements ChannelUpstreamHandler {

    private final GeolocationProvider geolocationProvider;
    private final boolean processInvalidPositions;

    public GeolocationHandler(GeolocationProvider geolocationProvider, boolean processInvalidPositions) {
        this.geolocationProvider = geolocationProvider;
        this.processInvalidPositions = processInvalidPositions;
    }

    @Override
    public void handleUpstream(final ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {
        if (!(evt instanceof MessageEvent)) {
            ctx.sendUpstream(evt);
            return;
        }

        final MessageEvent event = (MessageEvent) evt;
        Object message = event.getMessage();
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
                        Channels.fireMessageReceived(ctx, position, event.getRemoteAddress());
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        Log.warning(e);
                        Channels.fireMessageReceived(ctx, position, event.getRemoteAddress());
                    }
                });
            } else {
                Channels.fireMessageReceived(ctx, position, event.getRemoteAddress());
            }
        } else {
            Channels.fireMessageReceived(ctx, message, event.getRemoteAddress());
        }
    }

}
