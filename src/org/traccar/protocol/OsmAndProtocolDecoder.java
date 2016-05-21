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
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.joda.time.format.ISODateTimeFormat;
import org.traccar.BaseProtocolDecoder;
import org.traccar.Context;
import org.traccar.helper.Log;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

import com.ning.http.util.Base64;

public class OsmAndProtocolDecoder extends BaseProtocolDecoder {

	public static final String PARAM_ID = "id";
	public static final String PARAM_DEVICEID = "deviceid";
	public static final String PARAM_CIPHER = "cipher";

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
                    request.getContent().toString(StandardCharsets.US_ASCII), false);
            params = decoder.getParameters();
        }

		if (!setDeviceId(params, channel, remoteAddress))
			return null;

		Set<Entry<String, List<String>>> entrys;
		if (params.keySet().contains(PARAM_CIPHER))
			entrys = decodedEntrys(params.get(PARAM_CIPHER));
		else
			entrys = params.entrySet();

		if (entrys == null)
			return null;
		
        Position position = new Position();
        position.setProtocol(getProtocolName());
        position.setValid(true);
        
        for (Map.Entry<String, List<String>> entry : entrys) {
            String value = entry.getValue().get(0);
            switch (entry.getKey()) {
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
                        if (value.contains("T")) {
                            position.setTime(new Date(
                                    ISODateTimeFormat.dateTimeParser().parseMillis(value)));
                        } else {
                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            position.setTime(dateFormat.parse(value));
                        }
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

	private Set<Entry<String, List<String>>> decodedEntrys(List<String> ciphers)
	{
		if (ciphers == null || ciphers.isEmpty())
			return null;

		String cipher = ciphers.get(0);
		byte[] crypted = Base64.decode(cipher);
		
		Device device = Context.getIdentityManager().getDeviceById(getDeviceId());
		SecretKeySpec secretKeySpec = device.getSecretKeySpec();

		String decrypted = null;
		try
		{
			Cipher decrypter = Cipher.getInstance("AES");
			decrypter.init(Cipher.DECRYPT_MODE, secretKeySpec);
			byte[] cipherData = decrypter.doFinal(crypted);
			decrypted = new String(cipherData);
		} catch (Exception e)
		{
			Log.error("Exception '" + e.getMessage() + "' during decoding message for device '" + getDeviceId() + "'");
			return null;
		}

		QueryStringDecoder decoder = new QueryStringDecoder(decrypted);
		return decoder.getParameters().entrySet();
	}

	/**
	 * Trys to extract the device identifier from the given parameters. If no
	 * parameter with key "id" or "deviceid" is given this will return false. If
	 * a key with "id" or "deviceid" is given, these parameter will be removed
	 * from the given parameters and and hand over to the identify method.
	 * 
	 * @param parameters
	 * @param channel
	 * @param remoteAddress
	 * @return true, if a parameter with key "id" or "deviceid" is found and
	 *         this parameter could be identifyed.
	 */
	private boolean setDeviceId(Map<String, List<String>> parameters, Channel channel, SocketAddress remoteAddress)
	{
		List<String> idParameter = parameters.get(PARAM_ID);
		if (idParameter == null || idParameter.isEmpty())
			idParameter = parameters.get(PARAM_DEVICEID);

		if (idParameter == null || idParameter.isEmpty())
			return false;

		parameters.remove(PARAM_ID);
		parameters.remove(PARAM_DEVICEID);
		return identify(idParameter.get(0), channel, remoteAddress);
	}
}
