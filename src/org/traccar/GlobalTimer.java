/*
 * Copyright 2012 Anton Tananaev (anton@traccar.org)
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

import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

public final class GlobalTimer {

    private static Timer instance = null;

    private GlobalTimer() {
    }

    public static void release() {
        if (instance != null) {
            instance.stop();
        }
        instance = null;
    }

    public static Timer getTimer() {
        if (instance == null) {
            instance = new HashedWheelTimer();
        }
        return instance;
    }

}
