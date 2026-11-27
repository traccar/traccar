package org.traccar.mapmatcher;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MapMatcherTest {

    private final Client client = ClientBuilder.newClient();

    @Disabled
    @Test
    public void testTraccar() throws InterruptedException {
        MapMatcher mapMatcher = new TraccarMapMatcher(client, null, "");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<double[]> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        mapMatcher.getPoint(43.7384, 7.4246, new MapMatcher.MapMatcherCallback() {
            @Override
            public void onSuccess(double latitude, double longitude) {
                result.set(new double[]{latitude, longitude});
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable e) {
                error.set(e);
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertNull(error.get());
        assertEquals(43.7384, result.get()[0], 0.001);
        assertEquals(7.4246, result.get()[1], 0.001);
    }

}
