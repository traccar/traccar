package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.helper.Checksum;
import org.traccar.model.Command;

import java.nio.charset.StandardCharsets;

public class S5lProtocolEncoder extends BaseProtocolEncoder {

    public S5lProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private ByteBuf encodeContent(long deviceId, String content) {

        ByteBuf buf = Unpooled.buffer();

        buf.writeByte(0x78);
        buf.writeByte(0x78);

        buf.writeByte(1 + 1 + 4 + content.length() + 2 + 2); // message length

        buf.writeByte(S5lProtocolDecoder.MSG_COMMAND);

        buf.writeByte(4 + content.length()); // command length
        buf.writeInt(0); // server flag
        buf.writeBytes(content.getBytes(StandardCharsets.US_ASCII)); // command

        buf.writeShort(0); // information serial

        buf.writeShort(Checksum.crc16(Checksum.CRC16_X25, buf.nioBuffer(2, buf.writerIndex() - 2)));

        buf.writeByte('\r'); // 0x0D
        buf.writeByte('\n'); // 0x0A

        return buf;
    }

    @Override
    protected Object encodeCommand(Command command) {

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> encodeContent(command.getDeviceId(), command.getString(Command.KEY_DATA));
            case Command.TYPE_POSITION_SINGLE -> encodeContent(command.getDeviceId(), "WHERE#");
            case Command.TYPE_ENGINE_STOP -> encodeContent(command.getDeviceId(), "RELAY,1#");
            case Command.TYPE_ENGINE_RESUME -> encodeContent(command.getDeviceId(), "RELAY,0#");
            case Command.TYPE_REBOOT_DEVICE -> encodeContent(command.getDeviceId(), "RESET#");
            case Command.TYPE_FACTORY_RESET -> encodeContent(command.getDeviceId(), "FACTORY#");
            default -> null;
        };
    }

}
