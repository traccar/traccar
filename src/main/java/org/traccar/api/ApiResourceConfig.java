/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
package org.traccar.api;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.traccar.api.resource.ServerResource;
import org.traccar.api.security.SecurityRequestFilter;
import org.traccar.helper.ObjectMapperContextResolver;

public final class ApiResourceConfig {

    private ApiResourceConfig() {
    }

    public static ResourceConfig create() {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.property("jersey.config.server.wadl.disableWadl", true);
        resourceConfig.registerClasses(
                JacksonFeature.class,
                ObjectMapperContextResolver.class,
                DateParameterConverterProvider.class,
                SecurityRequestFilter.class,
                ResourceErrorHandler.class,
                StreamWriter.class);
        resourceConfig.packages(ServerResource.class.getPackage().getName());
        return resourceConfig;
    }

}
