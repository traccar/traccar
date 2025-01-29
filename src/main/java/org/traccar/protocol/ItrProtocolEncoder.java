package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.traccar.model.Command;

public class ItrProtocolEncoder extends MessageToByteEncoder<Command> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Command command, ByteBuf out) throws Exception {
        // Codifica comandos para o dispositivo
        ByteBuf buf = Unpooled.buffer();

        buf.writeShort(0x2828); // Marcador do pacote
        buf.writeByte(0x80); // PID para comandos
        buf.writeShort(0); // Tamanho do pacote (será atualizado)
        buf.writeShort(1); // Número de sequência

        // Adiciona o comando ao pacote
        String commandString = command.getType();
        buf.writeBytes(commandString.getBytes());

        // Atualiza o tamanho do pacote
        buf.setShort(3, buf.readableBytes() - 5);

        out.writeBytes(buf); // Envia o pacote
    }
}
