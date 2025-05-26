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

import com.google.inject.Singleton;
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
 * Coordinate Converter of Baidu Maps Open Platform
 * @see <a href="https://lbsyun.baidu.com/faq/api?title=webapi/guide/changeposition-base">Coordinate Conversion | Baidu Maps API SDK</a>
 * @see <a href="https://lbsyun.baidu.com/faq/api?title=webapi/appendix#sn%E8%AE%A1%E7%AE%97%E7%AE%97%E6%B3%95e">How to caculate sign | Baidu Maps API SDK</a>
 */
@Singleton
public class BaiduPositionConverter extends PositionConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaiduPositionConverter.class);

    private static final String API_URL = "https://api.map.baidu.com/geoconv/v2/";

    private static final int QPS = 3;
    private static final int MAX_POSITION_COUNT = 30;
    private static final int DAY_LIMIT = 5000;

    @Inject
    public BaiduPositionConverter(Storage storage, Config config, Client client) {
        super(storage, ConverterInfo.Baidu, client,
                config.getString(Keys.API_BAIDU_KEY), config.getString(Keys.API_BAIDU_SECRET));
    }

    @Override
    protected void setRequestParams(List<Position> positions, Map<String, Object> params) {
        params.put("ak", apiKey);
        params.put("coords", positionsToQueryParam(positions));
        params.put("model", 2);
        params.put("output", "json");
    }

    @Override
    protected Invocation.Builder createRequest(SignedRequestProvider request) {
        return request.requestWithSign("sn", true, true, true);
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
        int code = response.getInt("status", -1);
        if (code != 0) {
            throw new GeoConvException("Failed to request converted position (code: " + code + "), " +
                    "see: https://lbsyun.baidu.com/faq/api?title=webapi/guide/changeposition-base#%E6%9C%8D%E5%8A%A1%E7%8A%B6%E6%80%81%E7%A0%81");
        }
        try {
            LinkedList<ConvertedPosition> convertedPositions = new LinkedList<>();
            for (JsonObject location : response.getJsonArray("result").getValuesAs(JsonObject.class)) {
                ConvertedPosition position = new ConvertedPosition(info);
                position.setLatitude(location.getJsonNumber("y").doubleValue());
                position.setLongitude(location.getJsonNumber("x").doubleValue());
                convertedPositions.add(position);
            }
            return convertedPositions;
        } catch (Exception error) {
            throw new GeoConvException("Failed to parse converted position result.", error);
        }
    }
}