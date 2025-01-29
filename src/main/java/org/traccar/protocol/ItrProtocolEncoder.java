package org.traccar.protocol;

import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

public class ItrProtocolEncoder extends BaseProtocolEncoder {

    public ItrProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Channel channel, Object msg) {
        String command = (String) msg;
        ByteBuf buf = Unpooled.buffer();

        buf.writeShort(0x2828);
        buf.writeByte(0x80); // Código do pacote de comando
        buf.writeShort(command.length());
        buf.writeShort(1); // Número de sequência
        buf.writeBytes(command.getBytes());

        return buf;
    }
}
