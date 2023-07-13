/*
 * Copyright 2018 - 2019 Anton Tananaev (anton@traccar.org)
 * Copyright 2018 Andrey Kunitsyn (andrey@traccar.org)
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

import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;
import org.traccar.Protocol;
import org.traccar.config.Keys;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.helper.Checksum;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MegastekProtocolEncoder extends StringProtocolEncoder {

    public MegastekProtocolEncoder(Protocol protocol) {
        super(protocol);
    }
	
	public static String formatAddChecksumdOld(String string) {
        return String.format("$%s;%02X\\r\\n", string, Checksum.xor(string));
    }
    
    private String W051command(Command command) {
        DateFormat deviceDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        deviceDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return deviceDateFormat.format(new Date());
	}

    @Override
    protected Object encodeCommand(Command command) {
	    boolean alternative = AttributeUtil.lookup(getCacheManager(), Keys.PROTOCOL_ALTERNATIVE.withPrefix(getProtocolName()), command.getDeviceId());
	    
	    if(!alternative) {
		    // Protocol V2.00
	        switch (command.getType()) {
	            case Command.TYPE_CUSTOM:
	            	if (Command.KEY_DATA == "W051,now;") {
		            	return formatCommand(command, "$GPRS,%s;W051,%s;!", Command.KEY_UNIQUE_ID, W051command(command));
	            	} else {
		            	return formatCommand(command, "$GPRS,%s;%s!", Command.KEY_UNIQUE_ID, Command.KEY_DATA);
	            	}
	            case Command.TYPE_SET_CONNECTION:
	                return formatCommand(command, "$GPRS,%s;W003,%s,%s;!", Command.KEY_UNIQUE_ID, Command.KEY_SERVER, Command.KEY_PORT);
	            case Command.TYPE_SET_TIMEZONE:
	                return formatCommand(command, "$GPRS,%s;W020,%s;!", Command.KEY_UNIQUE_ID, Command.KEY_TIMEZONE);
	            case Command.TYPE_GET_DEVICE_STATUS:
	                return formatCommand(command, "$GPRS,%s;R029;!", Command.KEY_UNIQUE_ID);
	            case Command.TYPE_FACTORY_RESET:
	                return formatCommand(command, "$GPRS,%s;C099;!", Command.KEY_UNIQUE_ID);
	            case Command.TYPE_REBOOT_DEVICE:
	            	return formatCommand(command, "$GPRS,%s;W100;!", Command.KEY_UNIQUE_ID);
	            case Command.TYPE_POSITION_PERIODIC:
		            return formatCommand(command, "$GPRS,%s;W005,%s;!", Command.KEY_UNIQUE_ID, Command.KEY_FREQUENCY);
	            default:
	                return null;
	        }
        } else {
	        // Protocol V1.00
	        switch (command.getType()) {
		        case Command.TYPE_CUSTOM:
		            return formatAddChecksumdOld(formatCommand(command, ",%s,%s", Command.KEY_UNIQUE_ID, Command.KEY_DATA));
		        case Command.TYPE_POSITION_PERIODIC:
		        	return formatAddChecksumdOld(formatCommand(command, ",%s,0013,%s", Command.KEY_UNIQUE_ID, Command.KEY_FREQUENCY));
	            default:
	                return null;
	        }
        }
    }

}