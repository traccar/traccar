/*
 * Copyright 2024 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.handler;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.model.AttributeUtil;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class TimeHandlerTest {

    @Test
    public void testTimeOffset() {
        Config config = Mockito.mock(Config.class);
        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Device device = Mockito.mock(Device.class);
        
        // Configure mocks
        when(config.getString(Keys.TIME_OVERRIDE)).thenReturn("deviceTime");
        when(config.getString(Keys.TIME_PROTOCOLS)).thenReturn(null);
        when(config.hasKey(Keys.TIME_OFFSET)).thenReturn(true);
        
        // Setup device with time offset attribute
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("time.offset", 3600); // 1 hour offset
        when(device.getAttributes()).thenReturn(attributes);
        when(device.getGroupId()).thenReturn(0L);
        
        when(cacheManager.getObject(Device.class, 1L)).thenReturn(device);
        when(cacheManager.getConfig()).thenReturn(config);
        
        TimeHandler handler = new TimeHandler(config, cacheManager);
        
        Position position = new Position();
        position.setDeviceId(1L);
        Date originalTime = new Date(1000000000L); // Original time
        position.setDeviceTime(originalTime);
        position.setFixTime(originalTime);
        position.setServerTime(new Date());
        
        handler.onPosition(position, filtered -> {
            // Callback
        });
        
        // Check that time was adjusted by 1 hour (3600 seconds)
        assertEquals(1000000000L + 3600000L, position.getDeviceTime().getTime());
        assertEquals(1000000000L + 3600000L, position.getFixTime().getTime());
    }

    @Test
    public void testNegativeTimeOffset() {
        Config config = Mockito.mock(Config.class);
        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Device device = Mockito.mock(Device.class);
        
        // Configure mocks
        when(config.getString(Keys.TIME_OVERRIDE)).thenReturn("deviceTime");
        when(config.getString(Keys.TIME_PROTOCOLS)).thenReturn(null);
        when(config.hasKey(Keys.TIME_OFFSET)).thenReturn(true);
        
        // Setup device with negative time offset attribute
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("time.offset", -7200); // -2 hours offset
        when(device.getAttributes()).thenReturn(attributes);
        when(device.getGroupId()).thenReturn(0L);
        
        when(cacheManager.getObject(Device.class, 1L)).thenReturn(device);
        when(cacheManager.getConfig()).thenReturn(config);
        
        TimeHandler handler = new TimeHandler(config, cacheManager);
        
        Position position = new Position();
        position.setDeviceId(1L);
        Date originalTime = new Date(1000000000L); // Original time
        position.setDeviceTime(originalTime);
        position.setFixTime(originalTime);
        position.setServerTime(new Date());
        
        handler.onPosition(position, filtered -> {
            // Callback
        });
        
        // Check that time was adjusted by -2 hours (-7200 seconds)
        assertEquals(1000000000L - 7200000L, position.getDeviceTime().getTime());
        assertEquals(1000000000L - 7200000L, position.getFixTime().getTime());
    }
}