/*
 * Copyright 2016 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 Andrey Kunitsyn (andrey@traccar.org)
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

import java.util.Collection;

import org.traccar.helper.DateUtil;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Position;

public class GpxBuilder {

    private StringBuilder builder = new StringBuilder();
    private static final String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>"
            + "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"Traccar\" version=\"1.1\" "
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 "
            + "http://www.topografix.com/GPX/1/1/gpx.xsd\"><trk>\n";
    private static final String NAME = "<name>%1$s</name><trkseg>%n";
    private static final String POINT = "<trkpt lat=\"%1$f\" lon=\"%2$f\">"
            + "<time>%3$s</time>"
            + "<geoidheight>%4$f</geoidheight>"
            + "<course>%5$f</course>"
            + "<speed>%6$f</speed>"
            + "</trkpt>%n";
    private static final String FOOTER = "</trkseg></trk></gpx>";

    public GpxBuilder() {
        builder.append(HEADER);
        builder.append("<trkseg>\n");
    }

    public GpxBuilder(String name) {
        builder.append(HEADER);
        builder.append(String.format(NAME, name));
    }

    public void addPosition(Position position) {
        builder.append(String.format(POINT, position.getLatitude(), position.getLongitude(),
                DateUtil.formatDate(position.getFixTime()), position.getAltitude(),
                position.getCourse(), UnitsConverter.mpsFromKnots(position.getSpeed())));
    }

    public void addPositions(Collection<Position> positions) {
        for (Position position : positions) {
            addPosition(position);
        }
    }

    public String build() {
        builder.append(FOOTER);
        return builder.toString();
    }

}
