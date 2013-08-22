/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.traccar.BaseProtocolDecoder;
import org.traccar.ServerManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class OsmAndProtocolDecoder extends BaseProtocolDecoder {
    
    public OsmAndProtocolDecoder(ServerManager serverManager) {
        super(serverManager);
    }
    
    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {
        
        HttpRequest request = (HttpRequest) msg;
        QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
        Map<String, List<String>> params = decoder.getParameters();

        // Create new position
        Position position = new Position();
        ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter("osmand");

        // Identification
        String id = params.get("id").get(0);
        try {
            position.setDeviceId(getDataManager().getDeviceByImei(id).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + id);
        }

        // Decode position
        position.setValid(true);
        position.setTime(new Date(Long.valueOf(params.get("timestamp").get(0)) * 1000));
        position.setLatitude(Double.valueOf(params.get("lat").get(0)));
        position.setLongitude(Double.valueOf(params.get("lon").get(0)));

        // Optional parameters
        if (params.containsKey("speed")) {
            position.setSpeed(Double.valueOf(params.get("speed").get(0)));
        } else {
            position.setSpeed(0.0);
        }
        if (params.containsKey("bearing")) {
            position.setCourse(Double.valueOf(params.get("bearing").get(0)));
        } else {
            position.setCourse(0.0);
        }
        if (params.containsKey("altitude")) {
            position.setAltitude(Double.valueOf(params.get("altitude").get(0)));
        } else {
            position.setAltitude(0.0);
        }
        if (params.containsKey("hdop")) {
            extendedInfo.set("hdop", params.get("hdop").get(0));
        }

        position.setExtendedInfo(extendedInfo.toString());
        
        // Send response
        if (channel != null) {
            HttpResponse response = new DefaultHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            channel.write(response).addListener(ChannelFutureListener.CLOSE);
        }

        return position;
    }

}
