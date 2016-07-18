package org.traccar.protocol;

import org.traccar.Context;
import org.traccar.StringProtocolEncoder;
import org.traccar.helper.Log;
import org.traccar.model.Command;
import org.traccar.model.Device;

public class WondexProtocolEncoder extends StringProtocolEncoder {
    @Override
    protected Object encodeCommand(Command command) {

        command.set(Command.KEY_DEVICE_PASSWORD, "0000");
        if (Context.getDeviceManager() != null) {
            Device device = Context.getDeviceManager().getDeviceById(command.getDeviceId());
            if (device != null && device.getAttributes().containsKey(Command.KEY_DEVICE_PASSWORD)) {
                command.set(Command.KEY_DEVICE_PASSWORD, device.getAttributes()
                        .get(Command.KEY_DEVICE_PASSWORD).toString());
                }
        }

        switch (command.getType()) {
        case Command.TYPE_REBOOT_DEVICE:
            return formatCommand(command, "$WP+REBOOT={%s}", Command.KEY_DEVICE_PASSWORD);
        case Command.TYPE_POSITION_SINGLE:
            return formatCommand(command, "$WP+GETLOCATION={%s}", Command.KEY_DEVICE_PASSWORD);
        case Command.TYPE_IDENTIFICATION:
            return formatCommand(command, "$WP+VER={%s}", Command.KEY_DEVICE_PASSWORD);
        default:
            Log.warning(new UnsupportedOperationException(command.getType()));
            break;
        }

        return null;
    }

}
