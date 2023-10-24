package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.helper.DataConverter;
import org.traccar.model.Command;

/**
 * @author QingtaiJiang
 * @date 2023/9/19 14:17
 * @email qingtaij@163.com
 */
public class ZrProtocolEncoder extends BaseProtocolEncoder {

    public ZrProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Command command) {
        ByteBuf id = Unpooled.wrappedBuffer(DataConverter.parseHex(strHexPaddingLeft(getUniqueId(command.getDeviceId()), 20)));
        switch (command.getType()) {
            case Command.TYPE_SET_CONNECTION:
                return registrationInformation(id, command);
            case Command.TYPE_ALARM_SPEED:
                break;
        }
        return null;
    }

    private static ByteBuf registrationInformation(ByteBuf id, Command command) {
        ByteBuf request = Unpooled.buffer();
        ZrProtocolDecoder.add2391(request);

        request.writeShort(0x24c0);

        ByteBuf a24c0BytBuf = Unpooled.buffer();
        // 3: tcp long connection
        a24c0BytBuf.writeByte(3);
        a24c0BytBuf.writeByte(2);

        // (N+M)*2
        a24c0BytBuf.writeByte(1);
        a24c0BytBuf.writeShort(command.getInteger(Command.KEY_PORT));
        a24c0BytBuf.writeByte(command.getString(Command.KEY_SERVER).getBytes().length);
        a24c0BytBuf.writeBytes(command.getString(Command.KEY_SERVER).getBytes());

        a24c0BytBuf.writeByte(0);

        // sms gateway
        a24c0BytBuf.writeByte(0);

        a24c0BytBuf.writeInt(300);

        request.writeShort(a24c0BytBuf.readableBytes());
        request.writeBytes(a24c0BytBuf);

        return ZrProtocolDecoder.formatMessage(ZrProtocolDecoder.FRAME_TYPE_CFG, id, (short) 0x1040, (byte) 0, 1, request);
    }


    /**
     * If there are insufficient digits, add 0 from the left.
     */
    public static String strHexPaddingLeft(String data, int length) {
        int dataLength = data.length();
        if (dataLength < length) {
            StringBuilder dataBuilder = new StringBuilder(data);
            for (int i = dataLength; i < length; i++) {
                dataBuilder.insert(0, "0");
            }
            data = dataBuilder.toString();
        }
        return data;
    }
}
