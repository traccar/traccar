package org.traccar;

import org.traccar.config.Config;
import org.traccar.database.ConnectionManager;
import org.traccar.database.IdentityManager;
import org.traccar.database.MediaManager;
import org.traccar.database.StatisticsManager;
import org.traccar.model.Device;

import static org.mockito.Mockito.*;

public class BaseTest {

    static {
        Context.init(new TestIdentityManager());
    }

    protected <T extends BaseProtocolDecoder> T inject(T decoder) throws Exception {
        decoder.setConfig(new Config());
        var device = mock(Device.class);
        when(device.getId()).thenReturn(1L);
        var identityManager = mock(IdentityManager.class);
        when(identityManager.getByUniqueId(any())).thenReturn(device);
        decoder.setIdentityManager(identityManager);
        decoder.setConnectionManager(mock(ConnectionManager.class));
        decoder.setStatisticsManager(mock(StatisticsManager.class));
        decoder.setMediaManager(mock(MediaManager.class));
        return decoder;
    }

    protected <T extends BaseFrameDecoder> T inject(T decoder) throws Exception {
        return decoder;
    }

    protected <T extends BaseProtocolEncoder> T inject(T encoder) throws Exception {
        return encoder;
    }

}
