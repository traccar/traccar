/*
 * Copyright 2014 Jon S. Stumpf (http://github.com/jon-stumpf)
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.database.DataManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class Esky610ProtocolDecoder extends BaseProtocolDecoder {

    public Esky610ProtocolDecoder( DataManager dataManager,
                                   String protocol,
                                   Properties properties,
                                   boolean sendResponse ) {
        super(dataManager, protocol, properties);
    }

    // Login:  "EL;1;123456789012345;141012034708;"
    // Logins have a response but are not necessary.
    private static final Pattern patternLogin = Pattern.compile(
            "EL;1;"       +             // Header
            "(\\d{15});"  +             // IMEI
            "(\\d{12})"   +             // Time (YYMMDDhhmmss)
            ";"           );            // Undocumented ';'

    // Report: "EO;0;860111020115592;R;11+141007132350+42.17372+-76.31386+0.04+0+0x1+0+5243295"
    private static final Pattern patternReport = Pattern.compile(
            "EO;0;"                +    // Header
            "(\\d{15});"           +    // IMEI (some records don't have it (why?))
            "R;"                   +    // Report Header
            "(\\d{1,2})"           +    // Satellites
            "\\+(\\d{12})"         +    // Time (YYMMDDhhmmss)
            "\\+(-?\\d{1,2}.\\d+)" +    // Latitude
            "\\+(-?\\d{1,3}.\\d+)" +    // Longitude
            "\\+(\\d+.\\d+)"       +    // Speed (m/s)
            "\\+(\\d{1,3})"        +    // Heading (degrees)
            "\\+0x(\\p{XDigit})"   +    // Input State
            "\\+(\\d{1,2})"        +    // Message Type
            "\\+(\\d+)"            );   // Milage
            // "(?:\\+(d{1,5})"    +    // AD Value (not present. why?)
            // "(?:\\+(d{1,5}))?)?");   // Voltage (not present.  why?)
 
    @Override
    protected Object decode( ChannelHandlerContext ctx,
                             Channel channel,
                             SocketAddress remoteAddress,
                             Object msg ) throws Exception {

        String sentence = (String) msg;
        Matcher parser;
        
        Log.debug(getProtocol() + ": " + remoteAddress.toString() + " sent \"" + sentence + "\"");

        if ( (parser = patternReport.matcher(sentence)).matches() ) {
            Position position = new Position();
            ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter(getProtocol());
            int index = 1;

            // IMEI
            String imei = parser.group(index++);
            try {
                position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
            } catch(Exception error) {
                Log.warning("Unknown device - " + imei);
                return null;
            }

            // Satellites
            extendedInfo.set("satellites", parser.group(index++));

            // Date and Time
            DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
            position.setTime(df.parse(parser.group(index++)));
        
            // Latitude / Longitude / Validity
            position.setLatitude(Double.valueOf(parser.group(index++)));
            position.setLongitude(Double.valueOf(parser.group(index++)));

            position.setValid(! (position.getLatitude() == 0.0 && position.getLongitude() == 0.0));
            
            // Speed (m/s convert to knots)
            position.setSpeed(Double.valueOf(parser.group(index++)) * 1.94384);
            
            // Heading
            position.setCourse(Double.valueOf(parser.group(index++)));
            
            // Input State
            extendedInfo.set("inputState", parser.group(index++));
            
            // Message Type
            extendedInfo.set("messageType", parser.group(index++));
            
            // Mileage
            extendedInfo.set("milage", parser.group(index++));
            
            // Altitude (this information is not provided)
            // Traccar currently assumes this will be set to zero, not null
            position.setAltitude(0.0);

            // Extended info
            position.setExtendedInfo(extendedInfo.toString());

            return position;
        }
        else if ( (parser = patternLogin.matcher(sentence)).matches() ) {
            String imei = parser.group(1);
            String time = parser.group(2);
            
            Log.debug(getProtocol() + "(login): " + "imei=\"" + imei + "\", time=\"" + time);
            
            return null;
        }

        return null;
    }
}
