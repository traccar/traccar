/*
 * Copyright 2012 - 2025 Anton Tananaev (anton@traccar.org)
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

import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.traccar.config.Config;
import org.traccar.config.Keys;

@Singleton
public class EventLoopGroupFactory {

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    @Inject
    public EventLoopGroupFactory(Config config) {
        var ioHandlerFactory = NioIoHandler.newFactory();
        bossGroup = new MultiThreadIoEventLoopGroup(
                config.getInteger(Keys.SERVER_NETTY_BOSS_THREADS), ioHandlerFactory);
        workerGroup = new MultiThreadIoEventLoopGroup(
                config.getInteger(Keys.SERVER_NETTY_WORKER_THREADS), ioHandlerFactory);
    }

    public EventLoopGroup getBossGroup() {
        return bossGroup;
    }

    public EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

}
