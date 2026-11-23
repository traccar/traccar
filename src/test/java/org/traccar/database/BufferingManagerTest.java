package org.traccar.database;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import org.junit.jupiter.api.Test;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.model.Position;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BufferingManagerTest {

    private static Position position(long time) {
        Position position = new Position();
        position.setDeviceId(1);
        position.setTime(new Date(time));
        position.setServerTime(new Date());
        return position;
    }

    private static ChannelHandlerContext context(EventLoop eventLoop) {
        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        when(context.executor()).thenReturn(eventLoop);
        return context;
    }

    @Test
    public void testReleaseOrderAcrossConnections() throws Exception {
        Config config = new Config();
        config.setString(Keys.SERVER_BUFFERING_THRESHOLD, "100");

        List<Date> released = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(2);
        BufferingManager bufferingManager = new BufferingManager(config, (context, position) -> {
            released.add(position.getFixTime());
            done.countDown();
        });

        EventLoop busyLoop = new DefaultEventLoop();
        EventLoop idleLoop = new DefaultEventLoop();
        ChannelHandlerContext busyContext = context(busyLoop);
        ChannelHandlerContext idleContext = context(idleLoop);
        try {
            busyLoop.execute(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            });

            bufferingManager.accept(busyContext, position(1000));
            bufferingManager.accept(idleContext, position(2000));

            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertEquals(List.of(new Date(1000), new Date(2000)), released);
        } finally {
            busyLoop.shutdownGracefully();
            idleLoop.shutdownGracefully();
        }
    }

}
