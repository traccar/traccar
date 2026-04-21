/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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
package org.traccar.config;

public interface LookupContext {

    class Global implements LookupContext {
    }

    class User implements LookupContext {

        private final long userId;

        public long getUserId() {
            return userId;
        }

        public User(long userId) {
            this.userId = userId;
        }

    }

    class Device implements LookupContext {

        private final long deviceId;

        public long getDeviceId() {
            return deviceId;
        }

        public Device(long deviceId) {
            this.deviceId = deviceId;
        }

    }

}
