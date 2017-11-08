/*
 * Copyright 2012 - 2017 Anton Tananaev (anton@traccar.org)
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
import org.traccar.geocoder.Geocoder;
import org.traccar.helper.Log;
import org.traccar.model.Position;

public class GeocoderHandler implements ChannelUpstreamHandler {

    private final Geocoder geocoder;
    private final boolean processInvalidPositions;
    private final int geocoderReuseDistance;

    public GeocoderHandler(Geocoder geocoder, boolean processInvalidPositions) {
        this.geocoder = geocoder;
        this.processInvalidPositions = processInvalidPositions;

        geocoderReuseDistance = Context.getConfig().getInteger("geocoder.reuseDistance", 0);
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
            if (processInvalidPositions || position.getValid()) {
                if (geocoderReuseDistance != 0) {
                    Position lastPosition = Context.getIdentityManager().getLastPosition(position.getDeviceId());
                    if (lastPosition != null && lastPosition.getAddress() != null
                            && position.getDouble(Position.KEY_DISTANCE) <= geocoderReuseDistance) {
                        position.setAddress(lastPosition.getAddress());
                        Channels.fireMessageReceived(ctx, position, event.getRemoteAddress());
                        return;
                    }
                }

                Context.getStatisticsManager().registerGeocoderRequest();

                geocoder.getAddress(position.getLatitude(), position.getLongitude(),
                        new Geocoder.ReverseGeocoderCallback() {
                    @Override
                    public void onSuccess(String address) {
                        position.setAddress(address);
                        Channels.fireMessageReceived(ctx, position, event.getRemoteAddress());
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        Log.warning("Geocoding failed", e);
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
