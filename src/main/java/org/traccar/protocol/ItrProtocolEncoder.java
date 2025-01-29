package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ItrProtocolEncoder extends MessageToByteEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        // Implemente a codificação de comandos para o dispositivo
        if (msg instanceof String) {
            String command = (String) msg;
            ByteBuf buf = Unpooled.buffer();
            buf.writeShort(0x2828); // Marcador do pacote
            buf.writeByte(0x80); // PID para comandos
            buf.writeShort(command.length());
            buf.writeShort(1); // Número de sequência
            buf.writeBytes(command.getBytes(StandardCharsets.US_ASCII));
            out.writeBytes(buf);
        }
    }
}
