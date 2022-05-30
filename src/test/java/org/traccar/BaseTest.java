package org.traccar;

import org.traccar.config.Config;
import org.traccar.database.ConnectionManager;
import org.traccar.database.IdentityManager;
import org.traccar.database.MediaManager;
import org.traccar.database.StatisticsManager;
import org.traccar.model.Device;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseTest {

    protected <T extends BaseProtocolDecoder> T inject(T decoder) throws Exception {
        decoder.setConfig(new Config());
        var device = mock(Device.class);
        when(device.getId()).thenReturn(1L);
        var identityManager = mock(IdentityManager.class);
        when(identityManager.getById(anyLong())).thenReturn(device);
        when(identityManager.getByUniqueId(any())).thenReturn(device);
        when(identityManager.lookupAttributeBoolean(anyLong(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArguments()[2]);
        when(identityManager.lookupAttributeString(anyLong(), any(), any(), anyBoolean(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArguments()[2]);
        when(identityManager.lookupAttributeInteger(anyLong(), any(), anyInt(), anyBoolean(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArguments()[2]);
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
        var device = mock(Device.class);
        when(device.getId()).thenReturn(1L);
        when(device.getUniqueId()).thenReturn("123456789012345");
        var identityManager = mock(IdentityManager.class);
        when(identityManager.getDevicePassword(anyLong(), any(), any()))
                .thenAnswer(invocation -> invocation.getArguments()[2]);
        when(identityManager.getById(anyLong())).thenReturn(device);
        when(identityManager.getByUniqueId(any())).thenReturn(device);
        encoder.setIdentityManager(identityManager);
        return encoder;
    }

}
