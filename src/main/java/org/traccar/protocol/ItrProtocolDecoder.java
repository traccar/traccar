package org.traccar.protocol;

import org.traccar.BaseProtocolDecoder;
import org.traccar.helper.BitUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;
import java.util.Date;

public class ItrProtocolDecoder extends BaseProtocolDecoder {

    public ItrProtocolDecoder(ItrProtocol protocol) {
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
        position.setDeviceId(getDeviceId(channel, remoteAddress, String.valueOf(sequence)));

        // Decodifica o pacote com base no PID
        switch (pid) {
            case 0x01: // Pacote de login
                // Implemente a decodificação do pacote de login, se necessário
                break;

            case 0x12: // Pacote de localização
                position.setTime(new Date(buf.readUnsignedInt() * 1000L)); // Tempo UTC
                int mask = buf.readUnsignedByte(); // Máscara de dados válidos

                if (BitUtil.check(mask, 0)) { // Dados GPS válidos
                    position.setLatitude(buf.readInt() / 5000000.0); // Latitude
                    position.setLongitude(buf.readInt() / 5000000.0); // Longitude
                    position.setAltitude(buf.readShort()); // Altitude
                    position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedShort())); // Velocidade
                    position.setCourse(buf.readUnsignedShort()); // Curso
                    position.set(Position.KEY_SATELLITES, buf.readUnsignedByte()); // Número de satélites
                }

                // Decodifica outros campos, como status, bateria, etc.
                position.set(Position.KEY_STATUS, buf.readUnsignedShort()); // Status do dispositivo
                position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 1000.0); // Tensão da bateria
                break;

            default:
                return null; // Ignora pacotes desconhecidos
        }

        return position;
    }
}
