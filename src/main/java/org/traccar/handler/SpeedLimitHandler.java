/*
 * Copyright 2020 - 2022 Anton Tananaev (anton@traccar.org)
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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.model.Position;
import org.traccar.speedlimit.SpeedLimitProvider;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
public class SpeedLimitHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedLimitHandler.class);

    private final SpeedLimitProvider speedLimitProvider;

    @Inject
    public SpeedLimitHandler(SpeedLimitProvider speedLimitProvider) {
        this.speedLimitProvider = speedLimitProvider;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object message) {
        if (message instanceof Position) {
            final Position position = (Position) message;
            speedLimitProvider.getSpeedLimit(position.getLatitude(), position.getLongitude(),
                    new SpeedLimitProvider.SpeedLimitProviderCallback() {
                @Override
                public void onSuccess(double speedLimit) {
                    position.set(Position.KEY_SPEED_LIMIT, speedLimit);
                    ctx.fireChannelRead(position);
                }

                @Override
                public void onFailure(Throwable e) {
                    LOGGER.warn("Speed limit provider failed", e);
                    ctx.fireChannelRead(position);
                }
            });
        } else {
            ctx.fireChannelRead(message);
        }
    }

}
