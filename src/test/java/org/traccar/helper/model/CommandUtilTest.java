package org.traccar.helper.model;

import org.junit.jupiter.api.Test;
import org.traccar.model.Command;
import org.traccar.model.Device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CommandUtilTest {

    @Test
    public void testExpandUniqueIdPlaceholder() {
        Device device = new Device();
        device.setUniqueId("123456789012345");

        Command command = new Command();
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "$GPRS,{uniqueId};W005,1;!");

        CommandUtil.expandPlaceholders(command, device);

        assertEquals("$GPRS,123456789012345;W005,1;!", command.getString(Command.KEY_DATA));
    }

    @Test
    public void testExpandUniqueIdMultipleOccurrences() {
        Device device = new Device();
        device.setUniqueId("ABC");

        Command command = new Command();
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "*HQ,{uniqueId},X2,{uniqueId}#");

        CommandUtil.expandPlaceholders(command, device);

        assertEquals("*HQ,ABC,X2,ABC#", command.getString(Command.KEY_DATA));
    }

    @Test
    public void testIgnoreNonCustomCommands() {
        Device device = new Device();
        device.setUniqueId("123456789012345");

        Command command = new Command();
        command.setType(Command.TYPE_ENGINE_STOP);
        command.set(Command.KEY_DATA, "{uniqueId}");

        CommandUtil.expandPlaceholders(command, device);

        assertEquals("{uniqueId}", command.getString(Command.KEY_DATA));
    }

    @Test
    public void testIgnoreMissingData() {
        Device device = new Device();
        device.setUniqueId("123456789012345");

        Command command = new Command();
        command.setType(Command.TYPE_CUSTOM);

        CommandUtil.expandPlaceholders(command, device);

        assertNull(command.getString(Command.KEY_DATA));
    }

}
