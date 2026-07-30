/*
 * Copyright 2015 - 2026 Anton Tananaev (anton@traccar.org)
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

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.database.PositionBatchWriter;
import org.traccar.database.StatisticsManager;
import org.traccar.model.Position;

public class DatabaseHandler extends BasePositionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHandler.class);

    private final PositionBatchWriter batchWriter;
    private final StatisticsManager statisticsManager;

    @Inject
    public DatabaseHandler(PositionBatchWriter batchWriter, StatisticsManager statisticsManager) {
        this.batchWriter = batchWriter;
        this.statisticsManager = statisticsManager;
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        batchWriter.submit(position).whenComplete((id, error) -> {
            if (error == null) {
                position.setId(id);
                statisticsManager.registerMessageStored(position.getDeviceId(), position.getProtocol());
            } else {
                LOGGER.warn("Failed to store position", error);
            }
            callback.processed(false);
        });
    }

}
