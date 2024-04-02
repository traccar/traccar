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

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.DateCache;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.traccar.api.resource.SessionResource;

import java.util.Locale;
import java.util.TimeZone;

public class WebRequestLog extends ContainerLifeCycle implements RequestLog {

    private final Writer writer;

    private final DateCache dateCache = new DateCache(
            "dd/MMM/yyyy:HH:mm:ss ZZZ", Locale.getDefault(), TimeZone.getTimeZone("GMT"));

    public WebRequestLog(Writer writer) {
        this.writer = writer;
        addBean(writer);
    }

    @Override
    public void log(Request request, Response response) {
        try {
            Long userId = (Long) request.getSession().getAttribute(SessionResource.USER_ID_KEY);
            writer.write(String.format("%s - %s [%s] \"%s %s %s\" %d %d",
                    request.getRemoteHost(),
                    userId != null ? String.valueOf(userId) : "-",
                    dateCache.format(request.getTimeStamp()),
                    request.getMethod(),
                    request.getOriginalURI(),
                    request.getProtocol(),
                    response.getCommittedMetaData().getStatus(),
                    response.getHttpChannel().getBytesWritten()));
        } catch (Throwable ignored) {
        }
    }

}
