package org.traccar.protocol;

import org.traccar.BaseProtocol;
import org.traccar.http.commands.CommandType;
import org.traccar.protocol.commands.CommandTemplate;

import java.util.Map;

public class V680Protocol extends BaseProtocol {

    public V680Protocol() {
        super("v680");
    }

    @Override
    protected void loadCommandTemplates(Map<CommandType, CommandTemplate> templates) {
        
    }
}
