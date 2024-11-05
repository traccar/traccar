package org.traccar.protocol;

import io.netty.channel.Channel;
import org.traccar.BaseProtocolEncoder;
import org.traccar.Protocol;
import org.traccar.model.Command;

public class CalAmpProtocolEncoder extends BaseProtocolEncoder {
    public CalAmpProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    @Override
    protected Object encodeCommand(Channel channel, Command command) {
        return super.encodeCommand(channel, command);
    }
}
