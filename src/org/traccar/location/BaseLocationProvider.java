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
package org.traccar.location;

import org.traccar.Context;
import org.traccar.model.Event;

import java.util.Map;

public abstract class BaseLocationProvider implements LocationProvider {

    @Override
    public void getLocation(Map<String, Object> attributes, LocationProviderCallback callback) {

        Number mcc = (Number) attributes.get(Event.KEY_MCC);
        if (mcc == null) {
            mcc = Context.getConfig().getInteger("location.mcc");
        }

        Number mnc = (Number) attributes.get(Event.KEY_MNC);
        if (mnc == null) {
            mnc = Context.getConfig().getInteger("location.mnc");
        }

        Number lac = (Number) attributes.get(Event.KEY_LAC);
        Number cid = (Number) attributes.get(Event.KEY_CID);

        getLocation(mcc.intValue(), mnc.intValue(), lac.longValue(), cid.longValue(), callback);

    }

    protected abstract void getLocation(int mcc, int mnc, long lac, long cid, LocationProviderCallback callback);

}
