package org.traccar.flespi;

public class ChannelPullTask implements Runnable {

    private final FlespiClient flespiClient;

    protected ChannelPullTask(FlespiClient flespiClient) {
        this.flespiClient = flespiClient;
    }

    @Override
    public void run() {
        flespiClient.channelPull();
    }
}
