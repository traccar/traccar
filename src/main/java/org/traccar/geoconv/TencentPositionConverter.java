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

import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.SignedRequestProvider;
import org.traccar.model.ConvertedPosition;
import org.traccar.model.Position;
import org.traccar.storage.Storage;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Coordinate Converter of Tencent Location Service
 * @see <a href="https://lbs.qq.com/service/webService/webServiceGuide/webServiceTranslate">Tencent Location Service - WebService API: Coordinate Conversion</a>
 * @see <a href="https://lbs.qq.com/faq/serverFaq/webServiceKey">Tencent Location Service - How to caculate sign</a>
 */
public class TencentPositionConverter extends PositionConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TencentPositionConverter.class);

    private static final String API_URL = "https://apis.map.qq.com/ws/coord/v1/translate";

    private static final int QPS = 5;
    private static final int MAX_POSITION_COUNT = 80;
    private static final int DAY_LIMIT = 8000;

    @Inject
    public TencentPositionConverter(Storage storage, Config config, Client client) {
        super(storage, ConverterInfo.Tencent, client,
                config.getString(Keys.API_TENCENT_KEY), config.getString(Keys.API_TENCENT_SECRET));
    }

    @Override
    protected String positionToString(String latitude, String longitude) {
        return latitude + "," + longitude;
    }

    @Override
    protected void setRequestParams(List<Position> positions, Map<String, Object> params) {
        params.put("key", apiKey);
        params.put("locations", positionsToQueryParam(positions));
        params.put("type", 1);
        params.put("output", "json");
    }

    @Override
    protected Invocation.Builder createRequest(SignedRequestProvider request) {
        return request.requestWithSign("sig", true, false, false);
    }

    @Override
    protected SignedRequestProvider createSignedRequestProvider(String secretKey, Client client) {
        return new SignedRequestProvider(secretKey, client, API_URL);
    }

    @Override
    public int getMaxPositionPerRequest() {
        return MAX_POSITION_COUNT;
    }

    @Override
    public int getMaxRequestPerDay() {
        return DAY_LIMIT;
    }

    @Override
    public int getMaxRequestPerSec() {
        return QPS;
    }

    @Override
    protected List<ConvertedPosition> parseConvertedPosition(JsonObject response) throws GeoConvException {
        if (response.getInt("status", -1) != 0) {
            throw new GeoConvException(response.getString("message", "Unknown error."));
        }
        try {
            LinkedList<ConvertedPosition> convertedPositions = new LinkedList<>();
            for (JsonObject location : response.getJsonArray("locations").getValuesAs(JsonObject.class)) {
                ConvertedPosition position = new ConvertedPosition(info);
                position.setLatitude(location.getJsonNumber("lat").doubleValue());
                position.setLongitude(location.getJsonNumber("lng").doubleValue());
                convertedPositions.add(position);
            }
            return convertedPositions;
        } catch (Exception error) {
            throw new GeoConvException("Failed to parse converted position result.", error);
        }
    }
}
