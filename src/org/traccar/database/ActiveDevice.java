package org.traccar.database;

import org.jboss.netty.channel.Channel;

import java.net.SocketAddress;

public class ActiveDevice {
    private String uniqueId;
    private Channel channel;
    private SocketAddress remoteAddress;

    public ActiveDevice(String uniqueId, Channel channel, SocketAddress remoteAddress) {
        this.uniqueId = uniqueId;
        this.channel = channel;
        this.remoteAddress = remoteAddress;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void write(Object message) {
        getChannel().write(message, remoteAddress);
    }
}
