/*
 * Copyright 2012 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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

import org.traccar.geocode.AddressFormat;
import org.traccar.geocode.ReverseGeocoder;
import org.traccar.model.Position;

public class ReverseGeocoderHandler extends BaseDataHandler {

    private final ReverseGeocoder geocoder;
    private final boolean processInvalidPositions;
    private final AddressFormat addressFormat;

    public ReverseGeocoderHandler(ReverseGeocoder geocoder, boolean processInvalidPositions ) {
        this.geocoder = geocoder;
        this.processInvalidPositions = processInvalidPositions;
        addressFormat = new AddressFormat();
    }

    @Override
    protected Position handlePosition(Position position) {
        if (geocoder != null && (processInvalidPositions || position.getValid())) {
            position.setAddress(geocoder.getAddress(
                    addressFormat, position.getLatitude(), position.getLongitude()));
        }
        return position;
    }

}
