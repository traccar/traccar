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
package org.traccar.handler;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import java.time.Duration;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.database.StatisticsManager;
import org.traccar.geolocation.GeolocationProvider;
import org.traccar.model.Network;
import org.traccar.model.Position;

@ChannelHandler.Sharable
public class GeolocationHandler extends ChannelInboundHandlerAdapter {

    public static class LBSLocation {
        private double latitude, longitude, accuracy;

        public LBSLocation(double latitude, double longitude, double accuracy) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
        }

        public double getLatitude() {
            return this.latitude;
        }

        public double getLongitude() {
            return this.longitude;
        }

        public double getAccuracy() {
            return this.accuracy;
        }
    }


    private static final Logger LOGGER = LoggerFactory.getLogger(GeolocationHandler.class);

    private final GeolocationProvider geolocationProvider;
    private final StatisticsManager statisticsManager;
    private final boolean processInvalidPositions;
    private final CacheManager cacheManager;
    private final Cache<Network, LBSLocation> cache;

    public GeolocationHandler(
            Config config, GeolocationProvider geolocationProvider, StatisticsManager statisticsManager) {
        this.geolocationProvider = geolocationProvider;
        this.statisticsManager = statisticsManager;
        this.processInvalidPositions = config.getBoolean(Keys.GEOLOCATION_PROCESS_INVALID_POSITIONS);

        CacheConfiguration<Network, LBSLocation> cacheConfiguration = CacheConfigurationBuilder
            .newCacheConfigurationBuilder(Network.class, LBSLocation.class, ResourcePoolsBuilder.heap(1000))
            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(12)))
            .build();
        this.cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("lbsCache", cacheConfiguration)
            .build();
        this.cacheManager.init();
        this.cache = this.cacheManager.getCache("lbsCache", Network.class, LBSLocation.class);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object message) {
        if (message instanceof Position) {
            final Position position = (Position) message;
            if ((position.getOutdated() || processInvalidPositions && !position.getValid())
                    && position.getNetwork() != null) {
                if (statisticsManager != null) {
                    statisticsManager.registerGeolocationRequest();
                }
                LBSLocation cached = this.cache.get(position.getNetwork());
                if (cached != null) {
                        position.set(Position.KEY_APPROXIMATE, true);
                        position.setValid(true);
                        position.setFixTime(position.getDeviceTime());
                        position.setLatitude(cached.getLatitude());
                        position.setLongitude(cached.getLongitude());
                        position.setAccuracy(cached.getAccuracy());
                        position.setAltitude(0);
                        position.setSpeed(0);
                        position.setCourse(0);
                        ctx.fireChannelRead(position);
                } else {
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
                            GeolocationHandler.this.cache.put(position.getNetwork(),
                                                              new LBSLocation(latitude, longitude, accuracy));
                            ctx.fireChannelRead(position);
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            LOGGER.warn("Geolocation network error", e);
                            ctx.fireChannelRead(position);
                        }
                    });
                }
            } else {
                ctx.fireChannelRead(position);
            }
        } else {
            ctx.fireChannelRead(message);
        }
    }

}
