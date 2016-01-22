/*
 * Copyright 2013 - 2016 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.protocol;

import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.traccar.BaseProtocolDecoder;
import org.traccar.model.Event;
import org.traccar.model.Position;

public class OsmAndProtocolDecoder extends BaseProtocolDecoder {

    public OsmAndProtocolDecoder(OsmAndProtocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        HttpRequest request = (HttpRequest) msg;
        QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
        Map<String, List<String>> params = decoder.getParameters();
        if (params.isEmpty()) {
            decoder = new QueryStringDecoder(
                    request.getContent().toString(Charset.defaultCharset()), false);
            params = decoder.getParameters();
        }

        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setValid(true);

        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            String value = entry.getValue().get(0);
            switch (entry.getKey()) {
                case "id":
                case "deviceid":
                    if (!identify(value, channel, remoteAddress)) {
                        return null;
                    }
                    position.setDeviceId(getDeviceId());
                    break;
                case "valid":
                    position.setValid(Boolean.parseBoolean(value));
                    break;
                case "timestamp":
                    try {
                        long timestamp = Long.parseLong(value);
                        if (timestamp < Integer.MAX_VALUE) {
                            timestamp *= 1000;
                        }
                        position.setTime(new Date(timestamp));
                    } catch (NumberFormatException error) {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        position.setTime(dateFormat.parse(value));
                    }
                    break;
                case "lat":
                    position.setLatitude(Double.parseDouble(value));
                    break;
                case "lon":
                    position.setLongitude(Double.parseDouble(value));
                    break;
                case "speed":
                    position.setSpeed(Double.parseDouble(value));
                    break;
                case "bearing":
                case "heading":
                    position.setCourse(Double.parseDouble(value));
                    break;
                case "altitude":
                    position.setAltitude(Double.parseDouble(value));
                    break;
                case "hdop":
                    position.set(Event.KEY_HDOP, Double.parseDouble(value));
                    break;
                case "batt":
                    position.set(Event.KEY_BATTERY, value);
                    break;
                default:
                    position.set(entry.getKey(), value);
                    break;
            }
        }

        if (position.getFixTime() == null) {
            position.setTime(new Date());
        }

        if (channel != null) {
            HttpResponse response = new DefaultHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            channel.write(response).addListener(ChannelFutureListener.CLOSE);
        }

        return position;
    }

}
