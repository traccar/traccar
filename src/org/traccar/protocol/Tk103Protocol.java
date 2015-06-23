package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.http.commands.CommandType;
import org.traccar.protocol.commands.CommandTemplate;

import java.util.Map;

public class Tk103Protocol extends BaseProtocol {

    public Tk103Protocol() {
        super("tk103");
    }

    @Override
    protected void loadCommandTemplates(Map<CommandType, CommandTemplate> templates) {

    }
}
