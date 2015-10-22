package org.traccar;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.traccar.database.IdentityManager;
import org.traccar.helper.ChannelBufferTools;
import org.traccar.model.Device;
import org.traccar.protocol.GatorProtocol;
import org.traccar.protocol.GatorProtocolDecoder;

import java.net.*;
import java.util.concurrent.CyclicBarrier;

public class ChannelClosingTest {

    @BeforeClass
    public static void init() {
        Context.init(new IdentityManager() {

            private Device createDevice() {
                return null;
            }

            @Override
            public Device getDeviceById(long id) {
                return createDevice();
            }

            @Override
            public Device getDeviceByUniqueId(String imei) {
                return createDevice();
            }

        });
    }

    static class ExceptionWaiter implements ChannelUpstreamHandler {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        Channel channel;

        @Override
        public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
            ctx.sendUpstream(e);
            if (e instanceof ExceptionEvent) {
                channel = e.getChannel();
                barrier.await();
            }
        }

        void waitFor() throws Exception {
            barrier.await();
        }

        Channel channel() {
            return channel;
        }
    }

    @Test
    public void testUDP() throws Exception {
        final ExceptionWaiter exception = new ExceptionWaiter();

        TrackerServer udpServer = new TrackerServer(new ConnectionlessBootstrap(), "gator") {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                pipeline.addLast("objectDecoder", new GatorProtocolDecoder(new GatorProtocol()));
                pipeline.addLast("exceptionWaiter", exception);
            }
        };
        final int PORT = 50522;
        udpServer.setPort(PORT);
        try {
            udpServer.start();
            Assert.assertFalse(udpServer.getChannelGroup().isEmpty());
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.connect(InetAddress.getLocalHost(), PORT);
                byte[] data = ChannelBufferTools.convertHexString("242400");
                socket.send(new DatagramPacket(data, data.length));
            }
            exception.waitFor();
            Assert.assertFalse(udpServer.getChannelGroup().isEmpty());
            Assert.assertTrue(exception.channel().isBound());
            Assert.assertTrue(exception.channel().isOpen());
        } finally {
            udpServer.stop();
        }
    }

    @Test
    public void testTCP() throws Exception {
        final ExceptionWaiter exception = new ExceptionWaiter();
        TrackerServer tcpServer = new TrackerServer(new ServerBootstrap(), "gator") {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                pipeline.addLast("objectDecoder", new GatorProtocolDecoder(new GatorProtocol()));
                pipeline.addLast("exceptionWaiter", exception);
            }
        };
        final int PORT = 50522;
        tcpServer.setPort(PORT);
        try {
            tcpServer.start();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), PORT));

                byte[] data = ChannelBufferTools.convertHexString("242400");
                socket.getOutputStream().write(data);

                exception.waitFor();
                Assert.assertFalse(exception.channel().isBound());
                Assert.assertFalse(exception.channel().isOpen());
                Assert.assertFalse(exception.channel().isConnected());
            }
        } finally {
            tcpServer.stop();
        }
    }
}
