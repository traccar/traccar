/*
 * Copyright 2015 - 2024 Anton Tananaev (anton@traccar.org)
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.forward.PositionData;
import org.traccar.forward.PositionForwarder;
import org.traccar.forward.ResultHandler;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

public class PositionForwardingHandler extends BasePositionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PositionForwardingHandler.class);

    private static final String ATTRIBUTE_DEVICE_DISABLE_FORWARDING = "deviceDisableForwarding";

    private final CacheManager cacheManager;
    private final Timer timer;

    private final PositionForwarder positionForwarder;

    private final boolean retryEnabled;
    private final int retryDelay;
    private final int retryCount;
    private final int retryLimit;

    private final AtomicInteger deliveryPending;

    @Inject
    public PositionForwardingHandler(
            Config config, CacheManager cacheManager, Timer timer, @Nullable PositionForwarder positionForwarder) {

        this.cacheManager = cacheManager;
        this.timer = timer;
        this.positionForwarder = positionForwarder;

        this.retryEnabled = config.getBoolean(Keys.FORWARD_RETRY_ENABLE);
        this.retryDelay = config.getInteger(Keys.FORWARD_RETRY_DELAY);
        this.retryCount = config.getInteger(Keys.FORWARD_RETRY_COUNT);
        this.retryLimit = config.getInteger(Keys.FORWARD_RETRY_LIMIT);

        this.deliveryPending = new AtomicInteger();
    }

    class AsyncRequestAndCallback implements ResultHandler, TimerTask {

        private final PositionData positionData;

        private int retries = 0;

        AsyncRequestAndCallback(PositionData positionData) {
            this.positionData = positionData;
            deliveryPending.incrementAndGet();
        }

        private void send() {
            positionForwarder.forward(positionData, this);
        }

        private void retry(Throwable throwable) {
            boolean scheduled = false;
            try {
                if (retryEnabled && deliveryPending.get() <= retryLimit && retries < retryCount) {
                    schedule();
                    scheduled = true;
                }
            } finally {
                int pending = scheduled ? deliveryPending.get() : deliveryPending.decrementAndGet();
                LOGGER.warn("Position forwarding failed: " + pending + " pending", throwable);
            }
        }

        private void schedule() {
            timer.newTimeout(this, retryDelay * (long) Math.pow(2, retries++), TimeUnit.MILLISECONDS);
        }

        @Override
        public void onResult(boolean success, Throwable throwable) {
            if (success) {
                deliveryPending.decrementAndGet();
            } else {
                retry(throwable);
            }
        }

        @Override
        public void run(Timeout timeout) {
            boolean sent = false;
            try {
                if (!timeout.isCancelled()) {
                    send();
                    sent = true;
                }
            } finally {
                if (!sent) {
                    deliveryPending.decrementAndGet();
                }
            }
        }
    }

    @Override
    public void handlePosition(Position position, Callback callback) {
        if (positionForwarder != null) {
            Device device = cacheManager.getObject(Device.class, position.getDeviceId());

            Object disableForwarding = device.getAttributes().get(ATTRIBUTE_DEVICE_DISABLE_FORWARDING);
            if ((disableForwarding == null) || (!(Boolean) disableForwarding)) {
                PositionData positionData = new PositionData();
                positionData.setPosition(position);
                positionData.setDevice(device);
                new AsyncRequestAndCallback(positionData).send();
            } else {
                LOGGER.debug("Not forwarding event for device '{}' as forwarding is disabled for this device.",
                    device.getName());
            }
        }
        callback.processed(false);
    }

}
