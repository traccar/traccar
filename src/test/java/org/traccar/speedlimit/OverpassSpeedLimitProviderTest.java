package org.traccar.speedlimit;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class OverpassSpeedLimitProviderTest {

    @Ignore
    @Test
    public void test() throws Exception {
        testLocationProvider();
    }

    public void testLocationProvider() throws Exception {
        SpeedLimitProvider provider = new OverpassSpeedLimitProvider("http://8.8.8.8/api/interpreter");

        provider.getSpeedLimit(34.74767, -82.48098, new SpeedLimitProvider.SpeedLimitProviderCallback() {
            @Override
            public void onSuccess(double speedLimit) {
                assertEquals(52.1, speedLimit, 0.1);
            }

            @Override
            public void onFailure(Throwable e) {
                fail();
            }
        });

        Thread.sleep(Long.MAX_VALUE);
    }

}
