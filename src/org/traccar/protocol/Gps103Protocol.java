package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.http.commands.CommandType;
import org.traccar.http.commands.Duration;
import org.traccar.http.commands.FixPositioningCommand;
import org.traccar.http.commands.GpsCommand;
import org.traccar.protocol.commands.CommandTemplate;
import org.traccar.protocol.commands.CommandValueConversion;
import org.traccar.protocol.commands.StringCommandTemplate;

import java.util.Map;

public class Gps103Protocol extends BaseProtocol {

    public Gps103Protocol() {
        super("gps103");
    }

    @Override
    protected void loadCommandTemplates(Map<CommandType, CommandTemplate> templates) {
        templates.put(CommandType.STOP_POSITIONING, new StringCommandTemplate("**,imei:[%s],A", GpsCommand.UNIQUE_ID));
        templates.put(CommandType.FIX_POSITIONING, new StringCommandTemplate("**,imei:[%s],C,[%s]", GpsCommand.UNIQUE_ID, FixPositioningCommand.FREQUENCY)
                .addConverter(Duration.class, new CommandValueConversion<Duration>() {
                    @Override
                    public String convert(Duration value) {
                        return String.format("%02d%s", value.getValue(), value.getUnit().getCommandFormat());
                    }
                }));
        templates.put(CommandType.RESUME_ENGINE, new StringCommandTemplate("**,imei:[%s],J", GpsCommand.UNIQUE_ID));
        templates.put(CommandType.STOP_ENGINE, new StringCommandTemplate("**,imei:[%s],K", GpsCommand.UNIQUE_ID));
    }
}
