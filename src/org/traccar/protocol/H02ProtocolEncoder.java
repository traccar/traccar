/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.traccar.protocol;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.traccar.StringProtocolEncoder;
import org.traccar.helper.Log;
import org.traccar.model.Command;

/**
 *
 * @author andrey
 */
public class H02ProtocolEncoder extends StringProtocolEncoder {

    @Override
    protected Object encodeCommand(Command command) {

        // Temporary put default password
        command.set(Command.KEY_DEVICE_PASSWORD, "123456");
//        String devID="000000000000000";
        String devID="355488020137297";        
        SimpleDateFormat dateFormatUmc = new SimpleDateFormat("HHmmss");
        dateFormatUmc.setTimeZone(TimeZone.getTimeZone("UTC"));
        String time = dateFormatUmc.format(new Date());

        switch (command.getType()) {
            case Command.TYPE_POSITION_STOP:
                return formatCommand(command, "*HQ,%s,S26,%s#", devID, time);
//              return formatCommand(command, "*HQ,%s,A", devID);
//              return formatCommand(command, "*HQ,%s,S19,%s,A3,5,3,1#", devID, time);
            case Command.TYPE_POSITION_SINGLE:
                return formatCommand(command, "*HQ,%s,D1,%s#",devID);
            case Command.TYPE_POSITION_PERIODIC:
                return formatCommand(command, "*HQ,%s,S17,%s,%s#", devID, time, "60");                
            case Command.TYPE_ENGINE_STOP:
                return formatCommand(command, "*HQ,%s,S20,%s,1,3,10,3,5,5,3,5,3,5,3,5#", devID, time);
            case Command.TYPE_ENGINE_RESUME:
                return formatCommand(command, "*HQ,%s,S20,%s,0,0,0,0,0,0,0,0,0,0,0,0#", devID, time);
            case Command.TYPE_ALARM_IGNITION_ARM:
                return formatCommand(command, "*HQ,%s,S19,%s,A3,5,1,1#", devID, time);
            case Command.TYPE_ALARM_IGNITION_DISARM:
                return formatCommand(command, "*HQ,%s,S19,%s,A3,0,0,1#", devID, time);                
            case Command.TYPE_ALARM_DOOR_ARM:
                return formatCommand(command, "*HQ,%s,S19,%s,A1,5,1,1#", devID, time);
            case Command.TYPE_ALARM_DOOR_DISARM:
                return formatCommand(command, "*HQ,%s,S19,%s,A1,0,0,1#", devID, time);                


            case Command.TYPE_ALARM_ARM:
                return formatCommand(command, "*HQ,%s,S6,%s,0,0#", devID, time);
            case Command.TYPE_ALARM_DISARM:
                return formatCommand(command, "*HQ,%s,S6,%s,0,0#", devID, time);
            case Command.TYPE_ALARM_DROP:
                return formatCommand(command, "*HQ,%s,A1,%s#", devID, time);
            default:
                Log.warning(new UnsupportedOperationException(command.getType()));
                break;
        }

        return null;
    }
}