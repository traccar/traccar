/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
package org.traccar;

import org.traccar.helper.Log;
import org.traccar.model.Position;

public class DefaultDataHandler extends BaseDataHandler {

    @Override
    protected Position handlePosition(Position position) {

        try {
            Context.getDataManager().addPosition(position);
            Position lastPosition = Context.getConnectionManager().getLastPosition(position.getDeviceId());
            if (lastPosition == null || position.getFixTime().compareTo(lastPosition.getFixTime()) > 0) {
                Context.getDataManager().updateLatestPosition(position);
            }
        } catch (Exception error) {
            Log.warning(error);
        }

        return position;
    }

}
