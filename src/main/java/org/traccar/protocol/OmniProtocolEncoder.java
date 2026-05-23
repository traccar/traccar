package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.model.Command;

public class OmniProtocolEncoder extends BaseProtocolEncoder {

    private Channel activeChannel;

    public OmniProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        char previous = 0;
        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);
            if (character == '-' || character == ' ' || character == '.' || character == '/') {
                character = '_';
            }
            if (Character.isUpperCase(character) && Character.isLowerCase(previous)) {
                result.append('_');
            }
            result.append(character);
            previous = character;
        }
        return result.toString().trim().toUpperCase();
    }

    private static String commandNamePart(String data) {
        if (data == null) {
            return null;
        }
        String value = data.trim();
        if (value.startsWith("*") || value.startsWith("0xFFFF")) {
            return value;
        }
        int index = -1;
        for (char delimiter : new char[] {',', ':', ' '}) {
            int delimiterIndex = value.indexOf(delimiter);
            if (delimiterIndex >= 0 && (index < 0 || delimiterIndex < index)) {
                index = delimiterIndex;
            }
        }
        return index >= 0 ? value.substring(0, index) : value;
    }

    private static String valuePart(String data) {
        if (data == null) {
            return null;
        }
        String value = data.trim();
        int index = -1;
        for (char delimiter : new char[] {',', ':', '=', ' '}) {
            int delimiterIndex = value.indexOf(delimiter);
            if (delimiterIndex >= 0 && (index < 0 || delimiterIndex < index)) {
                index = delimiterIndex;
            }
        }
        return index >= 0 && index + 1 < value.length() ? value.substring(index + 1).trim() : null;
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return value != null && !value.trim().isEmpty() ? Integer.parseInt(value.trim()) : defaultValue;
        } catch (NumberFormatException error) {
            return defaultValue;
        }
    }

    private static String commandSuffix(String data) {
        String value = valuePart(data);
        return value != null && !value.isEmpty() ? "," + value : "";
    }

    private static String dataSuffix(Command command) {
        String value = command.getString(Command.KEY_DATA);
        return value != null && !value.trim().isEmpty() ? "," + value.trim() : "";
    }

    private String getVendor(Command command) {
        return firstNonEmpty(command.getString("vendor"), command.getString("vendorCode"), "OM");
    }

    private String getSenderId(Command command) {
        return firstNonEmpty(command.getString("senderId"), command.getString("userId"), "1234");
    }

    private int getSpeedLimitValue(Command command, int defaultValue) {
        int value = parseInt(firstNonEmpty(
                command.getString("value"),
                command.getString("speed"),
                command.getString("speedLimit"),
                command.getString("limit"),
                valuePart(command.getString(Command.KEY_DATA))), defaultValue);
        return Math.min(25, Math.max(6, value));
    }

    private ByteBuf formatCommand(Command command, String content) {
        if (activeChannel != null) {
            OmniProtocolDecoder decoder = activeChannel.pipeline().get(OmniProtocolDecoder.class);
            if (decoder != null) {
                return decoder.encodeCommand(getUniqueId(command.getDeviceId()), content);
            }
        }
        return OmniProtocolDecoder.encodeFrame(String.format(
                "*SCOS,%s,%s,%s#\n", getVendor(command), getUniqueId(command.getDeviceId()), content));
    }

    private ByteBuf formatOperationRequest(Command command, Channel channel, int operation) {
        if (channel != null) {
            OmniProtocolDecoder decoder = channel.pipeline().get(OmniProtocolDecoder.class);
            if (decoder != null) {
                decoder.setPendingCommand(command.getType());
            }
        }
        return formatCommand(command, String.format(
                "R0,%d,20,%s,%d", operation, getSenderId(command), System.currentTimeMillis() / 1000));
    }

    private ByteBuf formatNamedCommand(Command command, String name) {
        switch (normalizeName(name)) {
            case "UNLOCK":
                return formatCommand(command, String.format(
                        "R0,0,20,%s,%d", getSenderId(command), System.currentTimeMillis() / 1000));
            case "LOCK":
                return formatCommand(command, String.format(
                        "R0,1,20,%s,%d", getSenderId(command), System.currentTimeMillis() / 1000));
            case "UPDATE_VEHICLE_INFO":
                return formatCommand(command, "S6");
            case "UPDATE_GEO_INFO":
                return formatCommand(command, "D0");
            case "SETTINGS":
            case "IOT_SETTINGS":
                return formatCommand(command, "S5" + commandSuffix(command.getString(Command.KEY_DATA)));
            case "SCOOTER_SETTING_1":
                return formatCommand(command, "S7" + commandSuffix(command.getString(Command.KEY_DATA)));
            case "SCOOTER_SETTING_2":
                return formatCommand(command, "S4" + commandSuffix(command.getString(Command.KEY_DATA)));
            case "ENGINE_ON":
                return formatCommand(command, "S7,0,0,2,0");
            case "ENGINE_OFF":
                return formatCommand(command, "S7,0,0,1,0");
            case "MODE_ECO":
                return formatCommand(command, "S7,0,1,0,0");
            case "MODE_NORMAL":
                return formatCommand(command, "S7,0,2,0,0");
            case "MODE_SPORT":
                return formatCommand(command, "S7,0,3,0,0");
            case "HEADLIGHT_ON":
                return formatCommand(command, "S7,2,0,0,0");
            case "HEADLIGHT_OFF":
                return formatCommand(command, "S7,1,0,0,0");
            case "TAILLIGHT_FLASHING_ON":
                return formatCommand(command, "S7,0,0,0,2");
            case "TAILLIGHT_FLASHING_OFF":
                return formatCommand(command, "S7,0,0,0,1");
            case "INCH_SPEED_DISPLAY_ON":
                return formatCommand(command, "S4,2,0,0,0,0,0,0,0");
            case "INCH_SPEED_DISPLAY_OFF":
                return formatCommand(command, "S4,1,0,0,0,0,0,0,0");
            case "CRUISE_ON":
                return formatCommand(command, "S4,0,2,0,0,0,0,0,0");
            case "CRUISE_OFF":
                return formatCommand(command, "S4,0,1,0,0,0,0,0,0");
            case "BLOCK_CHANGE_SPEED_MODE_ON":
                return formatCommand(command, "S4,0,0,0,1,0,0,0,0");
            case "BLOCK_CHANGE_SPEED_MODE_OFF":
                return formatCommand(command, "S4,0,0,0,2,0,0,0,0");
            case "HEADLIGHT_CONTROL_ON":
                return formatCommand(command, "S4,0,0,0,0,2,0,0,0");
            case "HEADLIGHT_CONTROL_OFF":
                return formatCommand(command, "S4,0,0,0,0,1,0,0,0");
            case "SPEED_LIMIT": {
                int value = getSpeedLimitValue(command, 25);
                return formatCommand(command, String.format("S4,0,0,0,0,0,%d,%d,%d", value, value, value));
            }
            case "SPEED_LIMIT_ECO":
                return formatCommand(command, String.format(
                        "S4,0,0,0,0,0,%d,0,0", getSpeedLimitValue(command, 15)));
            case "SPEED_LIMIT_NORMAL":
                return formatCommand(command, String.format(
                        "S4,0,0,0,0,0,0,%d,0", getSpeedLimitValue(command, 20)));
            case "SPEED_LIMIT_SPORT":
                return formatCommand(command, String.format(
                        "S4,0,0,0,0,0,0,0,%d", getSpeedLimitValue(command, 25)));
            case "LOOK_FOR_ON":
                return formatCommand(command, "V0,2");
            case "BUZZER_CONTROL_ON":
                return formatCommand(command, "V0,1");
            case "BEEP":
            case "BEEP_ON":
                return formatCommand(command, "V0,1");
            case "BEEP_SETTINGS":
                return formatCommand(command, "V1" + commandSuffix(command.getString(Command.KEY_DATA)));
            case "REBOOT":
                return formatCommand(command, "S1,2");
            case "SET_GEO_INTERVAL_LOCKED":
                return formatCommand(command, "D1,300");
            case "SET_GEO_INTERVAL_5_SEC_UNLOCKED":
                return formatCommand(command, "D1,5");
            case "SET_GEO_INTERVAL_UNLOCKED":
                return formatCommand(command, "D1,10");
            case "UNLOCK_HELMET_LOCK":
                return formatCommand(command, "L5,2");
            case "LOCK_HELMET_LOCK":
                return formatCommand(command, "L5,18");
            case "GET_HELMET_LOCK_STATUS":
                return formatCommand(command, "L5,34");
            case "DISABLE_UNLOCK_STATUS_UPLOAD_INFORMATION":
                return formatCommand(command, "S5,0,1,0,0");
            case "REQUEST_BLE_KEY":
            case "BLE_KEY":
                return formatCommand(command, "K0");
            case "LIGHT_STRIP_ON":
                return formatCommand(command, "S2,1");
            case "LIGHT_STRIP_OFF":
                return formatCommand(command, "S2,0");
            case "GET_ICCID":
            case "ICCID":
                return formatCommand(command, "I0");
            case "GET_BLUETOOTH_MAC":
            case "GET_BLE_MAC":
            case "BLUETOOTH_MAC":
                return formatCommand(command, "M0");
            case "GET_RFID":
            case "RFID":
                return formatCommand(command, "C0");
            case "GET_BEACON":
            case "BEACON_VERIFY":
                return formatCommand(command, "B0");
            case "CONTROLLER_CUSTOM_DATA_Z0":
                return formatCommand(command, "Z0" + commandSuffix(command.getString(Command.KEY_DATA)));
            case "CONTROLLER_CUSTOM_DATA_Z1":
                return formatCommand(command, "Z1" + commandSuffix(command.getString(Command.KEY_DATA)));
            case "WIFI_POSITION":
            case "WIFI_ASSISTED_POSITION":
                return formatCommand(command, "D2");
            case "UPGRADE":
            case "UPGRADE_IOT":
                return formatCommand(command, "U0" + commandSuffix(command.getString(Command.KEY_DATA)));
            case "UPGRADE_CONTROLLER":
                return formatCommand(command, "U1" + commandSuffix(command.getString(Command.KEY_DATA)));
            case "UPGRADE_BMS":
                return formatCommand(command, "U2" + commandSuffix(command.getString(Command.KEY_DATA)));
            case "HTTP_UPGRADE":
                return formatCommand(command, "U5" + commandSuffix(command.getString(Command.KEY_DATA)));
            case "HTTP_UPGRADE_RESULT":
                return formatCommand(command, "U6" + commandSuffix(command.getString(Command.KEY_DATA)));
            default:
                return null;
        }
    }

    private ByteBuf formatCustomCommand(Command command) {
        String data = firstNonEmpty(command.getString(Command.KEY_DATA), command.getString("command"));
        if (data == null) {
            return formatCommand(command, "S6");
        }

        ByteBuf named = formatNamedCommand(command, commandNamePart(data));
        if (named != null) {
            return named;
        }

        data = data.replace("\\r", "\r").replace("\\n", "\n").replace("<LF>", "\n").trim();
        if (data.startsWith("0xFFFF")) {
            data = data.substring("0xFFFF".length()).trim();
        }
        if (data.startsWith("*")) {
            return OmniProtocolDecoder.encodeFrame(data.endsWith("\n") ? data : data + "\n");
        }
        if (data.endsWith("#")) {
            data = data.substring(0, data.length() - 1);
        }
        return formatCommand(command, data);
    }

    @Override
    protected Object encodeCommand(Channel channel, Command command) {

        activeChannel = channel;
        switch (command.getType()) {
            case Command.TYPE_CUSTOM:
                return formatCustomCommand(command);
            case Command.TYPE_IDENTIFICATION:
                return formatCommand(command, "I0");
            case Command.TYPE_POSITION_SINGLE:
                return formatCommand(command, "D0");
            case Command.TYPE_POSITION_PERIODIC:
                return formatCommand(command, "D1," + command.getInteger(Command.KEY_FREQUENCY));
            case Command.TYPE_POSITION_STOP:
                return formatCommand(command, "D1,0");
            case Command.TYPE_ENGINE_STOP:
            case Command.TYPE_ALARM_ARM:
                return formatOperationRequest(command, channel, 1);
            case Command.TYPE_ENGINE_RESUME:
            case Command.TYPE_ALARM_DISARM:
                return formatOperationRequest(command, channel, 0);
            case Command.TYPE_GET_VERSION:
                return formatCommand(command, "G0");
            case Command.TYPE_REBOOT_DEVICE:
                return formatCommand(command, "S1,2");
            case Command.TYPE_CONFIGURATION:
                return formatCommand(command, "S5" + dataSuffix(command));
            case Command.TYPE_OUTPUT_CONTROL:
                return formatCommand(command, "V0" + dataSuffix(command));
            case Command.TYPE_SET_INDICATOR:
                return formatCommand(command, "S2" + dataSuffix(command));
            case Command.TYPE_FIRMWARE_UPDATE:
                return formatCommand(command, "U0" + dataSuffix(command));
            case Command.TYPE_SET_SPEED_LIMIT: {
                int value = getSpeedLimitValue(command, 25);
                return formatCommand(command, String.format("S4,0,0,0,0,0,%d,%d,%d", value, value, value));
            }
            default:
                return formatNamedCommand(command, command.getType());
        }
    }
}
