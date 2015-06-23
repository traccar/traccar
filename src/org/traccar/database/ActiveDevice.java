package org.traccar.database;

import org.jboss.netty.channel.Channel;
import org.traccar.Protocol;
import org.traccar.http.commands.GpsCommand;

import java.net.SocketAddress;

public class ActiveDevice {
    private String uniqueId;
    private Protocol protocol;
    private Channel channel;
    private SocketAddress remoteAddress;

    public ActiveDevice(String uniqueId, Protocol protocol, Channel channel, SocketAddress remoteAddress) {
        this.uniqueId = uniqueId;
        this.protocol = protocol;
        this.channel = channel;
        this.remoteAddress = remoteAddress;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void sendCommand(GpsCommand command) {
        protocol.sendCommand(this, command);
    }

    public void write(Object message) {
        getChannel().write(message, remoteAddress);
    }
}
