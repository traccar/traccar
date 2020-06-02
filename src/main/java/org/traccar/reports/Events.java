/*
 * Copyright 2016 - 2018 Anton Tananaev (anton@traccar.org)
 * Copyright 2016 - 2018 Andrey Kunitsyn (andrey@traccar.org)
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import org.apache.poi.ss.util.WorkbookUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Geofence;
import org.traccar.model.Group;
import org.traccar.model.Maintenance;
import org.traccar.reports.model.DeviceReport;

public final class Events {
    private static final Logger LOGGER = LoggerFactory.getLogger(Events.class);

    private Events() {
    }

    public static Collection<Event> getObjects(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Collection<String> types, Date from, Date to) throws SQLException {
        ReportUtils.checkPeriodLimit(from, to);
        ArrayList<Event> result = new ArrayList<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            Collection<Event> events = Context.getDataManager().getEvents(deviceId, from, to);
            boolean all = types.isEmpty() || types.contains(Event.ALL_EVENTS);
            for (Event event : events) {
                if (all || types.contains(event.getType())) {
                    long geofenceId = event.getGeofenceId();
                    long maintenanceId = event.getMaintenanceId();
                    if ((geofenceId == 0 || Context.getGeofenceManager().checkItemPermission(userId, geofenceId))
                            && (maintenanceId == 0
                            || Context.getMaintenancesManager().checkItemPermission(userId, maintenanceId))) {
                       result.add(event);
                    }
                }
            }
        }
        return result;
    }

    public static void getExcel(OutputStream outputStream,
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Collection<String> types, Date from, Date to) throws SQLException, IOException {
        ReportUtils.checkPeriodLimit(from, to);
        ArrayList<DeviceReport> devicesEvents = new ArrayList<>();
        ArrayList<String> sheetNames = new ArrayList<>();
        HashMap<Long, String> geofenceNames = new HashMap<>();
        HashMap<Long, String> maintenanceNames = new HashMap<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            Collection<Event> events = Context.getDataManager().getEvents(deviceId, from, to);
            boolean all = types.isEmpty() || types.contains(Event.ALL_EVENTS);
            for (Iterator<Event> iterator = events.iterator(); iterator.hasNext();) {
                Event event = iterator.next();
                if (all || types.contains(event.getType())) {
                    long geofenceId = event.getGeofenceId();
                    long maintenanceId = event.getMaintenanceId();
                    if (geofenceId != 0) {
                        if (Context.getGeofenceManager().checkItemPermission(userId, geofenceId)) {
                            Geofence geofence = Context.getGeofenceManager().getById(geofenceId);
                            if (geofence != null) {
                                geofenceNames.put(geofenceId, geofence.getName());
                            }
                        } else {
                            iterator.remove();
                        }
                    } else if (maintenanceId != 0) {
                        if (Context.getMaintenancesManager().checkItemPermission(userId, maintenanceId)) {
                            Maintenance maintenance = Context.getMaintenancesManager().getById(maintenanceId);
                            if (maintenance != null) {
                                maintenanceNames.put(maintenanceId, maintenance.getName());
                            }
                        } else {
                            iterator.remove();
                        }
                    }
                } else {
                    iterator.remove();
                }
            }
            DeviceReport deviceEvents = new DeviceReport();
            Device device = Context.getIdentityManager().getById(deviceId);
            deviceEvents.setDeviceName(device.getName());
            sheetNames.add(WorkbookUtil.createSafeSheetName(deviceEvents.getDeviceName()));
            if (device.getGroupId() != 0) {
                Group group = Context.getGroupsManager().getById(device.getGroupId());
                if (group != null) {
                    deviceEvents.setGroupName(group.getName());
                }
            }
            deviceEvents.setObjects(events);
            devicesEvents.add(deviceEvents);
        }
        String templatePath = Context.getConfig().getString("report.templatesPath",
                "templates/export/");
        try (InputStream inputStream = new FileInputStream(templatePath + "/events.xlsx")) {
            org.jxls.common.Context jxlsContext = ReportUtils.initializeContext(userId);
            jxlsContext.putVar("devices", devicesEvents);
            jxlsContext.putVar("sheetNames", sheetNames);
            jxlsContext.putVar("geofenceNames", geofenceNames);
            jxlsContext.putVar("maintenanceNames", maintenanceNames);
            jxlsContext.putVar("from", from);
            jxlsContext.putVar("to", to);
            ReportUtils.processTemplateWithSheets(inputStream, outputStream, jxlsContext);
        }
    }

    public static void getPdf(OutputStream outputStream,
                              long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
                              Collection<String> types, Date from, Date to) throws SQLException, IOException {
        ReportUtils.checkPeriodLimit(from, to);
        PdfDocument pdf = new PdfDocument(new PdfWriter(outputStream));
        Document document = new Document(pdf, PageSize.A4);
        document.getPdfDocument().setDefaultPageSize(PageSize.A4.rotate());
        pdf.addEventHandler(PdfDocumentEvent.START_PAGE,
                new ReportUtils.TextFooterEventHandler(document, userId));

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        //title
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
        SimpleDateFormat repDate = new SimpleDateFormat("yyyy-MM-dd");
        LOGGER.warn("Creating pdf...");
        HashMap<Long, String> geofenceNames = new HashMap<>();
        HashMap<Long, String> maintenanceNames = new HashMap<>();
        for(long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            Collection<Event> events = Context.getDataManager()
                    .getEvents(deviceId, from, to);
            boolean all = types.isEmpty() || types.contains(Event.ALL_EVENTS);
            for (Iterator<Event> iterator = events.iterator(); iterator.hasNext();) {
                Event event = iterator.next();
                if (all || types.contains(event.getType())) {
                    long geofenceId = event.getGeofenceId();
                    long maintenanceId = event.getMaintenanceId();
                    if (geofenceId != 0) {
                        if (Context.getGeofenceManager().checkItemPermission(userId, geofenceId)) {
                            Geofence geofence = Context.getGeofenceManager().getById(geofenceId);
                            if (geofence != null) {
                                geofenceNames.put(geofenceId, geofence.getName());
                            }
                        } else {
                            iterator.remove();
                        }
                    } else if (maintenanceId != 0) {
                        if (Context.getMaintenancesManager().checkItemPermission(userId, maintenanceId)) {
                            Maintenance maintenance = Context.getMaintenancesManager().getById(maintenanceId);
                            if (maintenance != null) {
                                maintenanceNames.put(maintenanceId, maintenance.getName());
                            }
                        } else {
                            iterator.remove();
                        }
                    }
                } else {
                    iterator.remove();
                }
            }
            Device device = Context.getIdentityManager().getById(deviceId);
            Text title =
                    new Text("Report Type: Events").setFont(font).setFontSize(16f);
            Text name =
                    new Text("Device: "+device.getName()).setFont(font).setFontSize(15f);
            Text dates = new Text("Period: "+ repDate.format(from) +
                    " to " + repDate.format(to)).setFont(font).setFontSize(14f);
            Paragraph tit = new Paragraph().add(title);
            Paragraph date = new Paragraph().add(dates);
            Paragraph nam = new Paragraph().add(name);
            document.add(tit);
            document.add(nam);
            document.add(date);
            //body
            Table table = new Table(4).useAllAvailableWidth();
            Cell timeE = new Cell().add(new Paragraph("Time"))
                    .setBackgroundColor(ColorConstants.BLUE)
                    .setFontColor(ColorConstants.WHITE);
            Cell typeE = new Cell().add(new Paragraph("Type"))
                    .setBackgroundColor(ColorConstants.BLUE)
                    .setFontColor(ColorConstants.WHITE);
            Cell geoN = new Cell().add(new Paragraph("Geofence Name"))
                    .setBackgroundColor(ColorConstants.BLUE)
                    .setFontColor(ColorConstants.WHITE);
            Cell maintN = new Cell().add(new Paragraph("Maintenance Name"))
                    .setBackgroundColor(ColorConstants.BLUE)
                    .setFontColor(ColorConstants.WHITE);

            table.addCell(timeE);
            table.addCell(typeE);
            table.addCell(geoN);
            table.addCell(maintN);
            for (Event report : events) {
                LOGGER.warn("populating table...");
                Cell dtime = new Cell().add(new Paragraph(sdfTime.
                        format(report.getServerTime())));
                Cell type = new Cell().add(new Paragraph(report.getType()));
                Cell geofence, maintenance;
                try {
                    geofence = new Cell().add(new Paragraph(geofenceNames.
                            get(report.getGeofenceId())));
                    maintenance = new Cell().add(new Paragraph(maintenanceNames.
                            get(report.getMaintenanceId())));
                }catch (Exception ex){
                    LOGGER.error("Error: "+ex);
                    geofence = new Cell().add(new Paragraph(""));
                    maintenance = new Cell().add(new Paragraph(""));
                }
                //add cells to table
                table.addCell(dtime);
                table.addCell(type);
                table.addCell(geofence);
                table.addCell(maintenance);

            }
            document.add(table);
            document.add(new AreaBreak());
            LOGGER.warn("closing...");
        }
        //close pdf
        document.close();
    }
}
