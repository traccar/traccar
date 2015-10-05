/*
 * Copyright 2012 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.traccar.geocode.AddressFormat;
import org.traccar.geocode.ReverseGeocoder;
import org.traccar.model.Position;

public class ReverseGeocoderHandler implements ChannelUpstreamHandler {

    private final ReverseGeocoder geocoder;
    private final boolean processInvalidPositions;
    private final AddressFormat addressFormat;

    public ReverseGeocoderHandler(ReverseGeocoder geocoder, boolean processInvalidPositions) {
        this.geocoder = geocoder;
        this.processInvalidPositions = processInvalidPositions;
        addressFormat = new AddressFormat();
    }

    @Override
    public void handleUpstream(final ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {
        if (!(evt instanceof MessageEvent)) {
            ctx.sendUpstream(evt);
            return;
        }

        final MessageEvent e = (MessageEvent) evt;
        Object message = e.getMessage();
        if (message instanceof Position) {
            final Position position = (Position) message;
            if (geocoder != null && (processInvalidPositions || position.getValid())) {
                geocoder.getAddress(addressFormat, position.getLatitude(), position.getLongitude(),
                        new ReverseGeocoder.ReverseGeocoderCallback() {
                    @Override
                    public void onResult(String address) {
                        position.setAddress(address);
                        Channels.fireMessageReceived(ctx, position, e.getRemoteAddress());
                    }
                });
            }
        } else {
            Channels.fireMessageReceived(ctx, message, e.getRemoteAddress());
        }
    }

}
