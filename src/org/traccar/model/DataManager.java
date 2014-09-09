/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar.model;

import java.util.List;

/**
 * Data manager
 */
public interface DataManager {

    /**
     * Manage devices
     */
    public List<Device> getDevices() throws Exception;
    public Device getDeviceByImei(String imei) throws Exception;

    /**
     * Manage positions
     */
    public Long addPosition(Position position) throws Exception;
    public void updateLatestPosition(Position position, Long positionId) throws Exception;

}
