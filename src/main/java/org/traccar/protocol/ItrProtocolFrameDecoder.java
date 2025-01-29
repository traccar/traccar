package org.traccar.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class ItrProtocolFrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        // Verifica se há dados suficientes para ler o cabeçalho do pacote
        if (buf.readableBytes() < 7) {
            return;
        }

        // Verifica o marcador do pacote (0x28 0x28)
        if (buf.getUnsignedShort(buf.readerIndex()) != 0x2828) {
            buf.skipBytes(1); // Ignora bytes inválidos
            return;
        }

        int length = buf.getUnsignedShort(buf.readerIndex() + 3); // Tamanho do pacote

        // Verifica se o pacote está completo
        if (buf.readableBytes() >= length + 5) {
            out.add(buf.readRetainedSlice(length + 5)); // Adiciona o pacote completo à lista de saída
        }
    }
}
