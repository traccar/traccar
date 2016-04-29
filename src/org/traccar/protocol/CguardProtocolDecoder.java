/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.DateBuilder;
import org.traccar.model.Position;
import java.util.Date;
import java.net.SocketAddress;
import java.nio.charset.Charset;

public class CguardProtocolDecoder extends BaseProtocolDecoder {

    public CguardProtocolDecoder(CguardProtocol protocol) {
        super(protocol);
    }

    public static final int MSG_HEARTBEAT = 0x1A;
    public static final int MSG_DATA = 0x10;

    public static final String NV_DATA = "NV";
    public static final String NAN_DATA = "NAN";
    //public static
    //3a :
    //4e56 NV
    //4243 BC

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) msg;

        /*NV:160420 101902:55.799425:37.674033:0.94:NAN:213.59:156.6
        BC:160420 101903:CSQ1:74:NSQ1:10:BAT1:100*/
//4e56 3a 31363034323020313031393032 3a 35352e373939343235 3a 33372e363734303333 3a 302e39343a4e414e 3a 3231332e3539 3a 3135362e360a4e56 3a 31363034323020313031393033 3a 35352e3739312e3238 3a 4e414e 3a 3335392e3136 3a 3135362e360a 4243 3a 313630343230203130313930333a435351313a37343a4e5351313a31303a424154313a3130300a

         /*NV:160420 101902:55.799425:37.674033:0.94:NAN:213.59:156.6
        NV:160420 101903:55.791.28:NAN:359.16:156.6
        BC:160420 101903:CSQ1:74:NSQ1:10:BAT1:100*/
        String bufString = buf.toString(Charset.defaultCharset());
        String[] bufStringRows = bufString.split("\n");
        String dataString = bufStringRows[0];

        //String start = buf.toString(buf.readerIndex(), 2, Charset.defaultCharset()); buf.skipBytes(2);

        Position position = new Position();
        position.setProtocol(getProtocolName());

        if(dataString.startsWith(NV_DATA)) {
            //read all as String....
            String[] nvData = dataString.split(":");

            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyMMdd HHmmss");
            DateTime dt = formatter.parseDateTime(nvData[1]);
            position.setTime(dt.toDate());

            /*buf.skipBytes(1);  //3a :
            String strYear = buf.toString(buf.readerIndex(), 2, Charset.defaultCharset());        buf.skipBytes(2);
            String strMonth = buf.toString(buf.readerIndex(), 2, Charset.defaultCharset());        buf.skipBytes(2);
            String strDay = buf.toString(buf.readerIndex(), 2, Charset.defaultCharset());        buf.skipBytes(2);
            buf.skipBytes(1);  //3a :
            String strHour = buf.toString(buf.readerIndex(), 2, Charset.defaultCharset());        buf.skipBytes(2);
            String strMinute = buf.toString(buf.readerIndex(), 2, Charset.defaultCharset());        buf.skipBytes(2);
            String strSecond = buf.toString(buf.readerIndex(), 2, Charset.defaultCharset());        buf.skipBytes(2);
            int year = Integer.parseInt(strYear);
            int month = Integer.parseInt(strMonth);
            int day = Integer.parseInt(strDay);
            int hour = Integer.parseInt(strHour);
            int minute = Integer.parseInt(strMinute);
            int second = Integer.parseInt(strSecond);

            DateBuilder dateBuilder = new DateBuilder();
            dateBuilder.setDate(year, month, day);
            dateBuilder.setTime(hour, minute, second);
            position.setTime(dateBuilder.getDate());*/

            /*buf.skipBytes(1);  //3a :
            String strLatitude = buf.toString(buf.readerIndex(), 9, Charset.defaultCharset());        buf.skipBytes(9);
            double latitude = Double.parseDouble(strLatitude);*/
            double latitude = Double.parseDouble(nvData[2]);
            position.setLatitude(latitude);

            /*buf.skipBytes(1);  //3a :
            String strLongitude = buf.toString(buf.readerIndex(), 9, Charset.defaultCharset());        buf.skipBytes(9);
            double longitude = Double.parseDouble(strLongitude);*/
            double longitude = Double.parseDouble(nvData[3]);
            position.setLongitude(longitude);

            /*buf.skipBytes(1);  //3a :
            String strSpeed = buf.toString(buf.readerIndex(), 4, Charset.defaultCharset());        buf.skipBytes(4);
            double speed =  Double.parseDouble(strSpeed);*/
            double speed =  Double.parseDouble(nvData[4]);
            position.setSpeed(speed);

            /*buf.skipBytes(1);  //3a :
            String strAccuracy = buf.toString(buf.readerIndex(), 3, Charset.defaultCharset());
            if(!nvData[4].equalsIgnoreCase(NAN_DATA)) {
                buf.skipBytes(5);
            } else {
                buf.skipBytes(3);  //NAN + 3a :
            }*/

            /*buf.skipBytes(1);
            String strCource = buf.toString(buf.readerIndex(), 3, Charset.defaultCharset());
            if(!strCource.equalsIgnoreCase(NAN_DATA)) {
                strCource = buf.toString(buf.readerIndex(), 5, Charset.defaultCharset());
                buf.skipBytes(5);
                double cource = Double.parseDouble(strCource);
                position.setCourse(cource);
            } else {
                buf.skipBytes(3);  //NAN + 3a :
            } */
            if(!nvData[6].equalsIgnoreCase(NAN_DATA)) {
                double cource = Double.parseDouble(nvData[6]);
                position.setCourse(cource);
            }

            /* buf.skipBytes(1);
            String strAltitude = buf.toString(buf.readerIndex(), 3, Charset.defaultCharset());
            if(!strAltitude.equalsIgnoreCase(NAN_DATA)) {
                strAltitude = buf.toString(buf.readerIndex(), 4, Charset.defaultCharset());
                buf.skipBytes(5);
                double altitude = Double.parseDouble(strAltitude);
                position.setAltitude(altitude);
                buf.skipBytes(5);
            } else {
                buf.skipBytes(4);  //NAN + 3a :
            }*/
            if(!nvData[7].equalsIgnoreCase(NAN_DATA)) {
                double altitude = Double.parseDouble(nvData[7]);
                position.setAltitude(altitude);
            }
        } else {
            return null;
        }

        return position;
    }
}
