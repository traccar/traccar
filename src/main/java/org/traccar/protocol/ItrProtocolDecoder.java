package org.traccar.protocol;

import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class ItrProtocolDecoder extends BaseProtocolDecoder {

    public ItrProtocolDecoder(BaseProtocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        // Verifica o marcador do pacote (0x28 0x28)
        if (buf.readUnsignedShort() != 0x2828) {
            return null;
        }

        int pid = buf.readUnsignedByte(); // Identificador do pacote
        int size = buf.readUnsignedShort(); // Tamanho do pacote
        int sequence = buf.readUnsignedShort(); // Número de sequência

        Position position = new Position(getProtocolName());
        position.setDeviceId(getDeviceId(channel, remoteAddress, buf.toString(StandardCharsets.US_ASCII)));

        // Decodifica o pacote com base no PID
        switch (pid) {
            case 0x01: // Login package
                // Implemente a decodificação do pacote de login
                break;
            case 0x12: // Location package
                position.setTime(new Date(buf.readUnsignedInt() * 1000L)); // Tempo UTC
                int mask = buf.readUnsignedByte(); // Máscara de dados válidos

                if (BitUtil.check(mask, 0)) { // Dados GPS válidos
                    position.setLatitude(buf.readInt() / 5000000.0);
                    position.setLongitude(buf.readInt() / 5000000.0);
                    position.setAltitude(buf.readShort());
                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort()));
                    position.setCourse(buf.readUnsignedShort());
                    position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
                }

                // Adicione mais decodificações para outros campos, como status, bateria, etc.
                break;
            // Implemente outros casos para diferentes tipos de pacotes
            default:
                return null;
        }

        return position;
    }
}
