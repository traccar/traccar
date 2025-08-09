/*
 * Copyright 2023 - 2024 Anton Tananaev (anton@traccar.org)
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

import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.content.HttpContent;
import org.eclipse.jetty.http.content.ResourceHttpContentFactory;
import org.eclipse.jetty.server.ResourceService;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import jakarta.inject.Inject;
import java.io.File;
import java.nio.file.Path;

public class DefaultOverrideServlet extends DefaultServlet {

    private Resource overrideResource;

    @Inject
    public DefaultOverrideServlet(Config config) {
        String override = config.getString(Keys.WEB_OVERRIDE);
        if (override != null) {
            overrideResource = ResourceFactory.root().newResource(Path.of(new File(override).getPath()));
        }
    }

    @Override
    public ResourceService getResourceService() {
        ResourceService service = super.getResourceService();
        if (overrideResource != null) {
            HttpContent.Factory base = service.getHttpContentFactory();
            HttpContent.Factory first = new ResourceHttpContentFactory(overrideResource, new MimeTypes());
            service.setHttpContentFactory(path -> {
                String p = path.indexOf('.') < 0 ? "/" : path;
                HttpContent content = first.getContent(p);
                return content != null ? content : base.getContent(p);
            });
        }
        return service;
    }

}
