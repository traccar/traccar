package org.traccar.protocol;

import org.traccar.BaseProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.model.Position;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import java.net.SocketAddress;
import java.util.Date;

public class ItrProtocolDecoder extends BaseProtocolDecoder {

    public ItrProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        
        buf.skipBytes(2); // Marca inicial (0x2828)
        int pid = buf.readUnsignedByte(); // Tipo do pacote
        int length = buf.readUnsignedShort();
        int sequence = buf.readUnsignedShort();

        if (pid == 0x12) { // Pacote de localização
            Position position = new Position(getProtocolName());
            position.setDeviceId(getDeviceSession(channel, remoteAddress, buf.readBytes(8).toString()).getDeviceId());

            long time = buf.readUnsignedInt();
            position.setTime(new Date(time * 1000));

            int mask = buf.readUnsignedByte();
            if ((mask & 0x01) != 0) {  // Dados de GPS
                position.setLatitude(buf.readInt() / 1800000.0);
                position.setLongitude(buf.readInt() / 1800000.0);
                position.setAltitude(buf.readShort());
                position.setSpeed(buf.readUnsignedShort() * 1.852); // Convertendo para km/h
                position.setCourse(buf.readUnsignedShort());
                position.set(Position.KEY_SATELLITES, buf.readUnsignedByte());
            }

            position.set(Position.KEY_BATTERY, buf.readUnsignedShort() / 1000.0);
            position.set(Position.KEY_ODOMETER, buf.readUnsignedInt());
            position.set(Position.KEY_HOURS, buf.readUnsignedInt());

            return position;
        }

        return null;
    }
}
