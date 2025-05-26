/*
 * Copyright 2025 Haven Madray (sgpublic2002@gmail.com)
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
package org.traccar.geoconv;

import io.github.bucket4j.BlockingBucket;
import io.github.bucket4j.Bucket;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.handler.BasePositionHandler;
import org.traccar.helper.CoordinateUtil;
import org.traccar.helper.SignedRequestProvider;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.ConvertedPosition;
import org.traccar.model.Position;
import org.traccar.schedule.ScheduleTask;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Request;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public abstract class PositionConverter extends BasePositionHandler implements ScheduleTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(PositionConverter.class);

    private final Storage storage;
    private final Client client;
    protected final ConverterInfo info;
    protected final String apiKey;
    private final String secretKey;

    private final BlockingBucket rateLimiter;

    protected PositionConverter(Storage storage, ConverterInfo info, Client client, @Nullable String apiKey, @Nullable String secretKey) {
        this.storage = storage;
        this.info = info;
        this.client = client;
        this.apiKey = apiKey;
        this.secretKey = secretKey;

        this.rateLimiter = createRateLimiter();
    }

    protected String positionsToQueryParam(List<Position> positions) {
        return positionsToQueryParam(positions, ";");
    }

    protected String positionsToQueryParam(List<Position> positions, String delimiter) {
        StringJoiner joiner = new StringJoiner(delimiter);
        for (Position position : positions) {
            joiner.add(positionToString(
                    String.format("%.6f", position.getLatitude()),
                    String.format("%.6f", position.getLongitude())
            ));
        }
        return joiner.toString();
    }

    protected String positionToString(String latitude, String longitude) {
        return longitude + "," + latitude;
    }

    private BlockingBucket createRateLimiter() {
        int qps = getMaxRequestPerSec();
        int qpd = getMaxRequestPerDay();
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(qps)
                        .refillGreedy(qps, Duration.ofSeconds(1))
                        .initialTokens(0))
                .addLimit(limit -> limit
                        .capacity(qpd)
                        .refillIntervally(qpd, Duration.ofDays(1)))
                .build()
                .asBlocking();
    }

    protected abstract SignedRequestProvider createSignedRequestProvider(String secretKey, Client client);

    protected abstract void setRequestParams(List<Position> positions, Map<String, Object> params);

    protected abstract Invocation.Builder createRequest(SignedRequestProvider request);

    public abstract int getMaxPositionPerRequest();

    public abstract int getMaxRequestPerSec();

    public abstract int getMaxRequestPerDay();

    private List<ConvertedPosition> getConvertedPosition(@Nonnull List<Position> positions) throws GeoConvException {
        SignedRequestProvider request = createSignedRequestProvider(secretKey, client);
        setRequestParams(positions, request);
        Response resp = createRequest(request).get();
        List<ConvertedPosition> result = parseConvertedPosition(resp.readEntity(JsonObject.class));
        for (int i = 0; i < positions.size(); i++) {
            result.get(i).setId(positions.get(i).getId());
        }
        return result;
    }

    protected abstract List<ConvertedPosition> parseConvertedPosition(JsonObject response) throws GeoConvException;

    @Override
    public void run() {
        while (true) {
            List<Position> requestPositions;
            try {
                rateLimiter.consume(1);
                synchronized (this) {
                    requestPositions = PositionUtil.getLatestUnconvertedPositions(storage, info.getPlatform(), getMaxPositionPerRequest());
                    if (requestPositions.isEmpty()) {
                        LOGGER.debug("No positions of platform {} are waiting converting, wait for 30s...", info.getPlatform());
                        this.wait(30_000);
                        continue;
                    }
                }
            } catch (InterruptedException ignore) {
                break;
            } catch (Exception error) {
                LOGGER.error("Failed to get non-converted position from database", error);
                continue;
            }
            List<ConvertedPosition> convertedPositions;
            try {
                convertedPositions = getConvertedPosition(requestPositions);
            } catch (Exception error) {
                LOGGER.warn("Failed to request converted position", error);
                continue;
            }
            for (ConvertedPosition position : convertedPositions) {
                save(position);
            }
        }
    }

    public boolean enable() {
        return apiKey != null;
    }

    @Override
    public boolean multipleInstances() {
        return false;
    }

    @Override
    public void schedule(ScheduledExecutorService executor) {
        executor.schedule(this, 5, TimeUnit.SECONDS);
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        if (!enable()) {
            callback.processed(true);
            return;
        }
        if (CoordinateUtil.outOfChina(position.getLatitude(), position.getLongitude())) {
            callback.processed(true);
            return;
        }
        this.notify();
        callback.processed(false);
    }

    private void save(ConvertedPosition convertedPosition) {
        try {
            storage.addObject(convertedPosition, new Request(new Columns.All()));
            LOGGER.debug("Converted position stored, id: {}", convertedPosition.getId());
        } catch (Exception error) {
            LOGGER.warn("Failed to store converted position, id: {}", convertedPosition.getId(), error);
        }
    }

    public enum ConverterInfo {
        AutoNavi(AutoNaviPositionConverter.class, ConvertedPosition.PLATFORM_AUTONAVI, ConvertedPosition.CRS_GCJ_02),
        Baidu(BaiduPositionConverter.class, ConvertedPosition.PLATFORM_BAIDU, ConvertedPosition.CRS_BD_09),
        Tencent(TencentPositionConverter.class, ConvertedPosition.PLATFORM_TENCENT, ConvertedPosition.CRS_GCJ_02),
        ;

        private final Class<? extends PositionConverter> clazz;
        private final String platform;
        private final String crs;

        ConverterInfo(Class<? extends PositionConverter> clazz, String platform, String crs) {
            this.clazz = clazz;
            this.platform = platform;
            this.crs = crs;
        }

        public Class<? extends PositionConverter> getClazz() {
            return clazz;
        }

        public String getPlatform() {
            return platform;
        }

        public String getCrs() {
            return crs;
        }
    }

    public static Stream<Class<? extends PositionConverter>> all() {
        return Stream.of(ConverterInfo.values()).map(v -> v.clazz);
    }
}
