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
package org.traccar.protocol;

import java.util.Map;
import org.traccar.BaseProtocolEncoder;
import org.traccar.model.Command;

public class Gps103ProtocolEncoder extends BaseProtocolEncoder {

    /*@Override
    protected void initCommandsTemplates(Map<CommandType, CommandTemplate> templates) {
        templates.put(CommandType.FIX_POSITIONING, new StringCommandTemplate("**,imei:[%s],C,[%s]", Command.UNIQUE_ID, FixPositioningCommand.FREQUENCY)
                .addConverter(Duration.class, new CommandValueConversion<Duration>() {
                    @Override
                    public String convert(Duration value) {
                        return String.format("%02d%s", value.getValue(), value.getUnit().getCommandFormat());
                    }
                }));
    }*/

    private String formatCommand(String format, Command command) {
        
        String result = format;
        
        result = result.replaceAll("\\{uniqueId}", getUniqueId(command.getDeviceId()));
        for (Map.Entry<String, Object> entry : command.getOther().entrySet()) {
            result = result.replaceAll("\\{" + entry.getKey() + "}", entry.getValue().toString());
        }
        
        return result;
    }
    
    
    @Override
    protected Object encodeCommand(Command command) {
        
        switch (command.getType()) {
            case Command.TYPE_POSITION_STOP:
                return formatCommand("**,imei:{uniqueId},A", command);
            case Command.TYPE_POSITION_FIX:
                return formatCommand("**,imei:{uniqueId},C,{time}", command); // TODO
            case Command.TYPE_ENGINE_STOP:
                return formatCommand("**,imei:{uniqueId},K", command);
            case Command.TYPE_ENGINE_RESUME:
                return formatCommand("**,imei:{uniqueId},J", command);
        }
        
        return null;
    }
    
}
