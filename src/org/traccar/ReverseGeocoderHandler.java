/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.traccar.geocode.ReverseGeocoder;
import org.traccar.model.Position;

/**
 * Reverse geocoding channel event handler
 */
public class ReverseGeocoderHandler extends OneToOneDecoder {

    /**
     * Geocoder object
     */
    private ReverseGeocoder geocoder;

    public ReverseGeocoderHandler(ReverseGeocoder geocoder) {
        this.geocoder = geocoder;
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        if (msg instanceof Position) {
            Position position = (Position) msg;
            if (geocoder != null) {
                position.setAddress(geocoder.getAddress(
                        position.getLatitude(), position.getLongitude()));
            }
        }

        return msg;
    }

}
