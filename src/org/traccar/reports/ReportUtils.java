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
package org.traccar.reports;

import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.jxls.area.Area;
import org.jxls.builder.xls.XlsCommentAreaBuilder;
import org.jxls.common.CellRef;
import org.jxls.formula.StandardFormulaProcessor;
import org.jxls.transform.Transformer;
import org.jxls.transform.poi.PoiTransformer;
import org.jxls.util.TransformerFactory;
import org.traccar.Context;
import org.traccar.model.Position;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class ReportUtils {

    private ReportUtils() {
    }

    public static String getDistanceUnit(long userId) {
        return (String) Context.getPermissionsManager().lookupPreference(userId, "distanceUnit", "km");
    }

    public static String getSpeedUnit(long userId) {
        return (String) Context.getPermissionsManager().lookupPreference(userId, "speedUnit", "kn");
    }

    public static TimeZone getTimezone(long userId) {
        String timezone = (String) Context.getPermissionsManager().lookupPreference(userId, "timezone", null);
        return timezone != null ? TimeZone.getTimeZone(timezone) : TimeZone.getDefault();
    }

    public static Collection<Long> getDeviceList(Collection<Long> deviceIds, Collection<Long> groupIds) {
        Collection<Long> result = new ArrayList<>();
        result.addAll(deviceIds);
        for (long groupId : groupIds) {
            result.addAll(Context.getPermissionsManager().getGroupDevices(groupId));
        }
        return result;
    }

    public static double calculateDistance(Position firstPosition, Position lastPosition) {
        return calculateDistance(firstPosition, lastPosition, true);
    }

    public static double calculateDistance(Position firstPosition, Position lastPosition, boolean useOdometer) {
        double distance = 0.0;
        double firstOdometer = firstPosition.getDouble(Position.KEY_ODOMETER);
        double lastOdometer = lastPosition.getDouble(Position.KEY_ODOMETER);

        if (useOdometer && (firstOdometer != 0.0 || lastOdometer != 0.0)) {
            distance = lastOdometer - firstOdometer;
        } else if (firstPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)
                && lastPosition.getAttributes().containsKey(Position.KEY_TOTAL_DISTANCE)) {
            distance = lastPosition.getDouble(Position.KEY_TOTAL_DISTANCE)
                    - firstPosition.getDouble(Position.KEY_TOTAL_DISTANCE);
        }
        return distance;
    }

    public static String calculateFuel(Position firstPosition, Position lastPosition) {

        if (firstPosition.getAttributes().get(Position.KEY_FUEL_LEVEL) != null
                && lastPosition.getAttributes().get(Position.KEY_FUEL_LEVEL) != null) {

            BigDecimal value = new BigDecimal(firstPosition.getDouble(Position.KEY_FUEL_LEVEL)
                    - lastPosition.getDouble(Position.KEY_FUEL_LEVEL));
            return value.setScale(1, RoundingMode.HALF_EVEN).toString();
        }
        return null;
    }

    public static org.jxls.common.Context initializeContext(long userId) {
        org.jxls.common.Context jxlsContext = PoiTransformer.createInitialContext();
        jxlsContext.putVar("distanceUnit", getDistanceUnit(userId));
        jxlsContext.putVar("speedUnit", getSpeedUnit(userId));
        jxlsContext.putVar("webUrl", Context.getVelocityEngine().getProperty("web.url"));
        jxlsContext.putVar("dateTool", new DateTool());
        jxlsContext.putVar("numberTool", new NumberTool());
        jxlsContext.putVar("timezone", getTimezone(userId));
        jxlsContext.putVar("locale", Locale.getDefault());
        jxlsContext.putVar("bracketsRegex", "[\\{\\}\"]");
        return jxlsContext;
    }

    public static void processTemplateWithSheets(InputStream templateStream, OutputStream targetStream,
            org.jxls.common.Context jxlsContext) throws IOException {
        Transformer transformer = TransformerFactory.createTransformer(templateStream, targetStream);
        List<Area> xlsAreas = new XlsCommentAreaBuilder(transformer).build();
        for (Area xlsArea : xlsAreas) {
            xlsArea.applyAt(new CellRef(xlsArea.getStartCellRef().getCellName()), jxlsContext);
            xlsArea.setFormulaProcessor(new StandardFormulaProcessor());
            xlsArea.processFormulas();
        }
        transformer.deleteSheet(xlsAreas.get(0).getStartCellRef().getSheetName());
        transformer.write();
    }

}
