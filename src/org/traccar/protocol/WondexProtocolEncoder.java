package org.traccar.protocol;

import org.traccar.StringProtocolEncoder;
import org.traccar.helper.Log;
import org.traccar.model.Command;;

public class WondexProtocolEncoder extends StringProtocolEncoder {
    @Override
    protected Object encodeCommand(Command command) {

        // Temporary put default password
        command.set(Command.KEY_DEVICE_PASSWORD, "0000");

        switch (command.getType()) {
        case Command.TYPE_REBOOT_DEVICE:
            return formatCommand(command, "$WP+REBOOT={%s}", Command.KEY_DEVICE_PASSWORD);
        case Command.TYPE_POSITION_SINGLE:
            return formatCommand(command, "$WP+GETLOCATION={%s}", Command.KEY_DEVICE_PASSWORD);
        default:
            Log.warning(new UnsupportedOperationException(command.getType()));
            break;
        }

        return null;
    }

}
