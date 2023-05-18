/*
 * Copyright 2023 Anton Tananaev (anton@traccar.org)
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

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

public class ModernDefaultServlet extends DefaultServlet {

    private Resource overrideResource;

    @Inject
    public ModernDefaultServlet(Config config) {
        String override = config.getString(Keys.WEB_OVERRIDE);
        if (override != null) {
            overrideResource = Resource.newResource(new File(override));
        }
    }

    @Override
    public Resource getResource(String pathInContext) {
        if (overrideResource != null) {
            try {
                Resource override = overrideResource.addPath(pathInContext);
                if (override.exists()) {
                    return override;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return super.getResource(pathInContext.indexOf('.') < 0 ? "/" : pathInContext);
    }

    @Override
    public String getWelcomeFile(String pathInContext) {
        return super.getWelcomeFile("/");
    }

}
