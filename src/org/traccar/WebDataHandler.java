/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import com.ning.http.client.AsyncHttpClient;
import org.traccar.model.Position;

public class WebDataHandler extends BaseDataHandler {

    private final String url;

    public WebDataHandler(String url) {
        this.url = url;
    }

    @Override
    protected Position handlePosition(Position position) {
        String request = url.
                //replaceAll("\\{uniqueId}", String.valueOf(position.getDeviceId())).
                replaceAll("\\{deviceId}", String.valueOf(position.getDeviceId())).
                replaceAll("\\{fixTime}", String.valueOf(position.getFixTime().getTime())).
                replaceAll("\\{latitude}", String.valueOf(position.getLatitude())).
                replaceAll("\\{longitude}", String.valueOf(position.getLongitude()));
        
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        asyncHttpClient.prepareGet(request).execute();

        return position;
    }

}
