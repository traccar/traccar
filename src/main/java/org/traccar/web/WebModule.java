/*
 * Copyright 2022 - 2023 Anton Tananaev (anton@traccar.org)
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
package org.traccar.web;

import com.google.inject.servlet.ServletModule;
import org.traccar.api.AsyncSocketServlet;
import org.traccar.api.MediaFilter;

public class WebModule extends ServletModule {

    @Override
    protected void configureServlets() {
        filter("/*").through(OverrideTextFilter.class);
        filter("/api/*").through(ThrottlingFilter.class);
        filter("/api/media/*").through(MediaFilter.class);
        serve("/api/socket").with(AsyncSocketServlet.class);
    }
}
