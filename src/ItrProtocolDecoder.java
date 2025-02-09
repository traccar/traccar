package org.traccar.protocol;

import io.netty.channel.Channel;
import org.traccar.BaseProtocolDecoder;
import org.traccar.session.DeviceSession;
import org.traccar.Protocol;
import org.traccar.model.Position;

import java.net.SocketAddress;
import java.util.Date;
import java.util.regex.Pattern;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;

public class ItrProtocolDecoder extends BaseProtocolDecoder {

    private static final Pattern PATTERN = new PatternBuilder()
            .text("2828")              // Start bytes
            .number("(xx)")            // PID
            .number("(xx)")            // Size
            .number("(xx)")            // Sequence Number
            .expression("([0-9A-Fa-f]+)") // Payload (variável)
            .compile();

    public ItrProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

    private Position decodePayload(Channel channel, SocketAddress remoteAddress, String payload) {

        Position position = new Position(getProtocolName());

        // Exemplo de parsing para latitude e longitude
        String latitudeHex = payload.substring(10, 18);
        String longitudeHex = payload.substring(18, 26);

        double latitude = Integer.parseInt(latitudeHex, 16) / 1800000.0;
        double longitude = Integer.parseInt(longitudeHex, 16) / 1800000.0;

        position.setLatitude(latitude);
        position.setLongitude(longitude);
        position.setTime(new Date());

        return position;
    }

    @Override
    protected Object decode(Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;
        Parser parser = new Parser(PATTERN, sentence);

        if (!parser.matches()) {
            return null;
        }

        int pid = parser.nextInt(0);
        String payload = parser.next();

        return decodePayload(channel, remoteAddress, payload);
    }
}
