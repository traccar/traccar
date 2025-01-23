/*
 * Copyright 2015 - 2024 Anton Tananaev (anton@digitalegiz.org)
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
package org.digitalegiz.handler;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.digitalegiz.database.StatisticsManager;
import org.digitalegiz.model.Position;
import org.digitalegiz.storage.Storage;
import org.digitalegiz.storage.query.Columns;
import org.digitalegiz.storage.query.Request;

public class DatabaseHandler extends BasePositionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHandler.class);

    private final Storage storage;
    private final StatisticsManager statisticsManager;

    @Inject
    public DatabaseHandler(Storage storage, StatisticsManager statisticsManager) {
        this.storage = storage;
        this.statisticsManager = statisticsManager;
    }

    @Override
    public void onPosition(Position position, Callback callback) {

        try {
            position.setId(storage.addObject(position, new Request(new Columns.Exclude("id"))));
            statisticsManager.registerMessageStored(position.getDeviceId(), position.getProtocol());
        } catch (Exception error) {
            LOGGER.warn("Failed to store position", error);
        }

        callback.processed(false);
    }

}
