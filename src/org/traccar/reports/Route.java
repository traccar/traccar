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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.jxls.area.Area;
import org.jxls.builder.xls.XlsCommentAreaBuilder;
import org.jxls.common.CellRef;
import org.jxls.formula.StandardFormulaProcessor;
import org.jxls.transform.Transformer;
import org.jxls.transform.poi.PoiTransformer;
import org.jxls.util.TransformerFactory;
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Position;
import org.traccar.reports.model.DeviceReport;

public final class Route {

    private Route() {
    }

    public static Collection<Position> getObjects(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws SQLException {
        ArrayList<Position> result = new ArrayList<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            result.addAll(Context.getDataManager().getPositions(deviceId, from, to));
        }
        return result;
    }

    public static void getExcel(OutputStream outputStream,
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            DateTime from, DateTime to) throws SQLException, IOException {
        ArrayList<DeviceReport> devicesRoutes = new ArrayList<>();
        ArrayList<String> sheetNames = new ArrayList<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            Collection<Position> positions = Context.getDataManager()
                    .getPositions(deviceId, from.toDate(), to.toDate());
            DeviceReport deviceRoutes = new DeviceReport();
            Device device = Context.getIdentityManager().getDeviceById(deviceId);
            deviceRoutes.setDeviceName(device.getName());
            sheetNames.add(deviceRoutes.getDeviceName());
            if (device.getGroupId() != 0) {
                Group group = Context.getDeviceManager().getGroupById(device.getGroupId());
                if (group != null) {
                    deviceRoutes.setGroupName(group.getName());
                }
            }
            deviceRoutes.setObjects(positions);
            devicesRoutes.add(deviceRoutes);
        }
        String templatePath = Context.getConfig().getString("report.templatesPath",
                "templates/export/");
        try (InputStream inputStream = new FileInputStream(templatePath + "/route.xlsx")) {
            org.jxls.common.Context jxlsContext = PoiTransformer.createInitialContext();
            jxlsContext.putVar("devices", devicesRoutes);
            jxlsContext.putVar("sheetNames", sheetNames);
            jxlsContext.putVar("from", from);
            jxlsContext.putVar("to", to);
            jxlsContext.putVar("distanceUnit", ReportUtils.getDistanceUnit(userId));
            jxlsContext.putVar("speedUnit", ReportUtils.getSpeedUnit(userId));
            jxlsContext.putVar("timezone", from.getZone());
            jxlsContext.putVar("bracketsRegex", "[\\{\\}\"]");
            Transformer transformer = TransformerFactory.createTransformer(inputStream, outputStream);
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
}
