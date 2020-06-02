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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

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
import com.itextpdf.layout.property.AreaBreakType;
import com.itextpdf.layout.property.UnitValue;
import org.apache.poi.ss.util.WorkbookUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.Main;
import org.traccar.database.DeviceManager;
import org.traccar.database.IdentityManager;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.reports.model.DeviceReport;
import org.traccar.reports.model.TripReport;

public final class Trips {
    private static final Logger LOGGER = LoggerFactory.getLogger(Trips.class);
    private Trips() {
    }

    private static Collection<TripReport> detectTrips(long deviceId, Date from, Date to) throws SQLException {
        boolean ignoreOdometer = Context.getDeviceManager()
                .lookupAttributeBoolean(deviceId, "report.ignoreOdometer", false, false, true);

        IdentityManager identityManager = Main.getInjector().getInstance(IdentityManager.class);
        DeviceManager deviceManager = Main.getInjector().getInstance(DeviceManager.class);

        return ReportUtils.detectTripsAndStops(
                identityManager, deviceManager, Context.getDataManager().getPositions(deviceId, from, to),
                Context.getTripsConfig(), ignoreOdometer, TripReport.class);
    }

    public static Collection<TripReport> getObjects(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws SQLException {
        ReportUtils.checkPeriodLimit(from, to);
        ArrayList<TripReport> result = new ArrayList<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            result.addAll(detectTrips(deviceId, from, to));
        }
        return result;
    }

    public static void getExcel(OutputStream outputStream,
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws SQLException, IOException {
        ReportUtils.checkPeriodLimit(from, to);
        ArrayList<DeviceReport> devicesTrips = new ArrayList<>();
        ArrayList<String> sheetNames = new ArrayList<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            Collection<TripReport> trips = detectTrips(deviceId, from, to);
            DeviceReport deviceTrips = new DeviceReport();
            Device device = Context.getIdentityManager().getById(deviceId);
            deviceTrips.setDeviceName(device.getName());
            sheetNames.add(WorkbookUtil.createSafeSheetName(deviceTrips.getDeviceName()));
            if (device.getGroupId() != 0) {
                Group group = Context.getGroupsManager().getById(device.getGroupId());
                if (group != null) {
                    deviceTrips.setGroupName(group.getName());
                }
            }
            deviceTrips.setObjects(trips);
            devicesTrips.add(deviceTrips);
        }
        String templatePath = Context.getConfig().getString("report.templatesPath",
                "templates/export/");
        try (InputStream inputStream = new FileInputStream(templatePath + "/trips.xlsx")) {
            org.jxls.common.Context jxlsContext = ReportUtils.initializeContext(userId);
            jxlsContext.putVar("devices", devicesTrips);
            jxlsContext.putVar("sheetNames", sheetNames);
            jxlsContext.putVar("from", from);
            jxlsContext.putVar("to", to);
            ReportUtils.processTemplateWithSheets(inputStream, outputStream, jxlsContext);
        }
    }

    public static void getPdf(OutputStream outputStream,
                              long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
                              Date from, Date to) throws SQLException, IOException {
        ReportUtils.checkPeriodLimit(from, to);
        PdfDocument pdf = new PdfDocument(new PdfWriter(outputStream));
        Document document = new Document(pdf, PageSize.A4);
        document.getPdfDocument().setDefaultPageSize(PageSize.A4.rotate());
        pdf.addEventHandler(PdfDocumentEvent.START_PAGE,
                new ReportUtils.TextFooterEventHandler(document, userId));

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        //title
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
        SimpleDateFormat repDate = new SimpleDateFormat("dd-MM-yyyy");
        LOGGER.warn("Creating pdf...");
        for(long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Device device = Context.getIdentityManager().getById(deviceId);
            Text title =
                    new Text("Vehiclewise Trip report ").setFont(font).setFontSize(10f);
            Text name =
                    new Text("Vehicle No. "+device.getName()).setFont(font).setFontSize(10f);
            Text dates = new Text("Period: "+ repDate.format(from) +
                    " to " + repDate.format(to)).setFont(font).setFontSize(10f);
            Paragraph tit = new Paragraph().add(title).add("      ").add(dates);
            Paragraph nam = new Paragraph().add(name);
            document.add(tit);
            //body
            Collection<TripReport> trips = detectTrips(deviceId, from, to);
            //ArrayList stores Trip report details in order of date
            ArrayList<Collection<TripReport>> datedTrips = new ArrayList<>();
            TripReport prevTrip = null;
            //stores Trip dates
            ArrayList<Date> tripDates = new ArrayList<>();
            //arraylist to be added to datedTrips
            Collection<TripReport> tripCollection = new ArrayList<>();
            //iterator to iterate through trips
            Iterator<TripReport> tripIter= trips.iterator();
            while(tripIter.hasNext()){
                TripReport trip = tripIter.next();
                if(prevTrip != null) {
                    if (!repDate.format(prevTrip.getTripDate())
                            .equals(repDate.format(trip.getTripDate()))) {
                        datedTrips.add(tripCollection);
                        tripCollection = new ArrayList<>();
                        tripDates.add(trip.getTripDate());
                    }
                    prevTrip = trip;
                    tripCollection.add(trip);
                    if(!trips.iterator().hasNext()){
                        datedTrips.add(tripCollection);
                    }
                }else{
                    tripDates.add(trip.getTripDate());
                    prevTrip = trip;
                    tripCollection.add(trip);
                }
                if(!tripIter.hasNext()){
                    datedTrips.add(tripCollection);
                }
            }
            /**sort by driver*/
            ArrayList<Collection<TripReport>> driversDiff = new ArrayList<>();
            for(Collection<TripReport> drivers : datedTrips){
                Iterator<TripReport> iterator = drivers.iterator();
                sortByDriver(iterator, driversDiff);
            }
            /***********************************************************************/
            float[] colWidths = {1,1,1,1,2,2,1,1,1};
            int count = 0;
            for(Collection<TripReport> repColl:driversDiff) {
                Table table = new Table(UnitValue.createPercentArray(colWidths))
                        .useAllAvailableWidth();
                /** Add date on each page */
                Text oneDate =
                        new Text("Date: "+repDate.format(tripDates.get(count))).
                                setFont(font).setFontSize(10f);
                Paragraph oneDatePar = new Paragraph().add(oneDate).add("      ").add(name);
                document.add(oneDatePar);
                /*************************************************************************/
                createTableHeaders(table);
                for (TripReport report : repColl) {
                    LOGGER.warn("populating table...");
                    Cell startTime = new Cell().add(new Paragraph(sdfTime.
                            format(report.getStartTime())).setFontSize(8f));
                    Cell sAddr, eAddr;
                    try {

                        sAddr = new Cell().add(new Paragraph(report.getStartAddress()).setFontSize(6f));
                        eAddr = new Cell().add(new Paragraph(report.getEndAddress()).setFontSize(6f));
                    } catch (Exception ex) {
                        LOGGER.error("Error: " + ex);
                        sAddr = new Cell().add(new Paragraph(report.getStartLat()
                                + "," + report.getStartLon()).setFontSize(8f));
                        eAddr = new Cell().add(new Paragraph(report.getEndLat()
                                + "," + report.getEndLon()).setFontSize(8f));
                    }
                    Cell endTime = new Cell().add(new Paragraph(sdfTime.
                            format(report.getEndTime())).setFontSize(8f));
                    Cell distance = new Cell().add(new Paragraph(String.format("%.2f",
                            report.getDistance() * 0.001) + "km").setFontSize(8f));
                    Cell topSpeed = new Cell().add(new Paragraph(Math.round(report
                            .getMaxSpeed() * 1.852) + "km/h").setFontSize(8f));
                    Cell duration = new Cell().add(new Paragraph(String.format("%dhr %dmin",
                            TimeUnit.MILLISECONDS.toHours(report.getDuration()),
                            (report.getDuration() / (60 * 1000)) % 60)).setFontSize(8f));
                    Cell idleTime = new Cell().add(new Paragraph(String.format("%dhr %dmin",
                            TimeUnit.MILLISECONDS.toHours(report.getIdleTime()),
                            (report.getIdleTime() / (60 * 1000)) % 60)).setFontSize(8f));
                    Cell driver;
                    try {
                        driver = new Cell().add(new Paragraph(report.getDriverName()).setFontSize(8f));
                    } catch (Exception ex) {
                        LOGGER.warn("Exception DRIVER: " + ex);
                        driver = new Cell().add(new Paragraph(""));
                    }
                    //add cells to table
                    table.addCell(startTime);
                    table.addCell(endTime);
                    table.addCell(idleTime);
                    table.addCell(driver);
                    table.addCell(sAddr);
                    table.addCell(eAddr);
                    table.addCell(duration);
                    table.addCell(distance);
                    table.addCell(topSpeed);


                }
                document.add(table);
                document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                count++;
            }
        }
        LOGGER.warn("closing...");
        //close pdf
        document.setMargins(0,0,0,0);
        document.close();
    }

    private static void createTableHeaders(Table table){
        Cell deviceSTm = new Cell().add(new Paragraph("Start Time").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceSAddr = new Cell().add(new Paragraph("Start Address").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceETm = new Cell().add(new Paragraph("End Time").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceEAddr = new Cell().add(new Paragraph("End Address").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceDuration = new Cell().add(new Paragraph("Duration").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceDst = new Cell().add(new Paragraph("Distance").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceTopSpeed = new Cell().add(new Paragraph("Top Speed").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceDriver = new Cell().add(new Paragraph("Driver").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceIdlTme = new Cell().add(new Paragraph("Idle Time").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);

        table.addHeaderCell(deviceSTm);
        table.addHeaderCell(deviceETm);
        table.addHeaderCell(deviceIdlTme);
        table.addHeaderCell(deviceDriver);
        table.addHeaderCell(deviceSAddr);
        table.addHeaderCell(deviceEAddr);
        table.addHeaderCell(deviceDuration);
        table.addHeaderCell(deviceDst);
        table.addHeaderCell(deviceTopSpeed);
    }

    private static void createDriverTableHeaders(Table table){
        Cell deviceDriver = new Cell().add(new Paragraph("Driver").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceSTm = new Cell().add(new Paragraph("Start Time").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceETm = new Cell().add(new Paragraph("End Time").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceDuration = new Cell().add(new Paragraph("Distance").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceEAddr = new Cell().add(new Paragraph("End Address").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);


        table.addHeaderCell(deviceSTm);
        table.addHeaderCell(deviceETm);
        table.addHeaderCell(deviceDuration);
        table.addHeaderCell(deviceEAddr);
        table.addHeaderCell(deviceDriver);

    }

    private static void sortByDriver(Iterator<TripReport> iterator, ArrayList<Collection<TripReport>> report){
        Collection<TripReport> collection = new ArrayList<>();
        TripReport prevTrip = null;
        while(iterator.hasNext()){
            TripReport trip = iterator.next();
            if(prevTrip != null){
                try{
                    if((!trip.getDriverName().equals("")&&!prevTrip.getDriverName().equals("")) &&
                            (!trip.getDriverName().equals(prevTrip.getDriverName()))){
                        report.add(collection);
                        collection = new ArrayList<>();
                    }
                    collection.add(trip);
                    prevTrip = trip;
                }catch (Exception e){
                    LOGGER.warn("REPORT GEN DRIVER: "+e);
                    collection.add(trip);
                }
            }else{
                prevTrip = trip;
                collection.add(trip);
            }
            if(!iterator.hasNext()){
                report.add(collection);
            }
        }
    }

    public static void getDriverPDF(OutputStream outputStream,
                                    long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
                                    Date from, Date to) throws SQLException, IOException {
        ReportUtils.checkPeriodLimit(from, to);
        PdfDocument pdf = new PdfDocument(new PdfWriter(outputStream));
        Document document = new Document(pdf, PageSize.A4);
        document.getPdfDocument().setDefaultPageSize(PageSize.A4.rotate());
        pdf.addEventHandler(PdfDocumentEvent.START_PAGE,
                new ReportUtils.TextFooterEventHandler(document, userId));

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        //title
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
        SimpleDateFormat repDate = new SimpleDateFormat("dd-MM-yyyy");
        LOGGER.warn("Creating pdf...");
        for (long deviceId : ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Device device = Context.getIdentityManager().getById(deviceId);
            Text title =
                    new Text("Vehiclewise Driver report ").setFont(font).setFontSize(10f);
            Text name =
                    new Text("Vehicle: " + device.getName()).setFont(font).setFontSize(10f);
            Text dates = new Text("Period: " + repDate.format(from) +
                    " to " + repDate.format(to)).setFont(font).setFontSize(10f);
            Paragraph tit = new Paragraph().add(title).add("      ").add(dates);
            Paragraph nam = new Paragraph().add(name);
            document.add(tit);
            //body
            Collection<TripReport> trips = detectTrips(deviceId, from, to);
            //ArrayList stores Trip report details in order of date
            ArrayList<Collection<TripReport>> datedTrips = new ArrayList<>();
            TripReport prevTrip = null;
            //stores Trip dates
            ArrayList<Date> tripDates = new ArrayList<>();
            //arraylist to be added to datedTrips
            Collection<TripReport> tripCollection = new ArrayList<>();
            //iterator to iterate through trips
            Iterator<TripReport> tripIter = trips.iterator();
            while (tripIter.hasNext()) {
                TripReport trip = tripIter.next();
                if (prevTrip != null) {
                    if (!repDate.format(prevTrip.getTripDate())
                            .equals(repDate.format(trip.getTripDate()))) {
                        datedTrips.add(tripCollection);
                        tripCollection = new ArrayList<>();
                        tripDates.add(trip.getTripDate());
                    }
                    prevTrip = trip;
                    tripCollection.add(trip);
                    if (!trips.iterator().hasNext()) {
                        datedTrips.add(tripCollection);
                    }
                } else {
                    tripDates.add(trip.getTripDate());
                    prevTrip = trip;
                    tripCollection.add(trip);
                }
                if (!tripIter.hasNext()) {
                    datedTrips.add(tripCollection);
                }
            }
            /**sort by driver*/
            ArrayList<Collection<TripReport>> driversDiff = new ArrayList<>();
            for (Collection<TripReport> drivers : datedTrips) {
                Iterator<TripReport> iterator = drivers.iterator();
                sortByDriver(iterator, driversDiff);
            }
            /***********************************************************************/
            float[] colWidths = {1, 1, 1, 4, 1};
            int count = 0;
            for (Collection<TripReport> repColl : driversDiff) {
                Table table = new Table(UnitValue.createPercentArray(colWidths))
                        .useAllAvailableWidth();
                /** Add date on each page */
                Text oneDate =
                        new Text("Date: " + repDate.format(tripDates.get(count))).
                                setFont(font).setFontSize(10f);
                Paragraph oneDatePar = new Paragraph().add(oneDate).add("      ").add(name);
                document.add(oneDatePar);
                /*************************************************************************/
                createDriverTableHeaders(table);
                for (TripReport report : repColl) {
                    LOGGER.warn("populating table...");
                    Cell startTime = new Cell().add(new Paragraph(sdfTime.
                            format(report.getStartTime())).setFontSize(8f));
                    Cell eAddr;
                    try {

                        eAddr = new Cell().add(new Paragraph(report.getEndAddress()).setFontSize(6f));
                    } catch (Exception ex) {
                        LOGGER.error("Error: " + ex);
                        eAddr = new Cell().add(new Paragraph(report.getEndLat()
                                + "," + report.getEndLon()).setFontSize(8f));
                    }
                    Cell endTime = new Cell().add(new Paragraph(sdfTime.
                            format(report.getEndTime())).setFontSize(8f));
                    Cell distance = new Cell().add(new Paragraph(String.format("%.2f",
                            report.getDistance() * 0.001) + "km").setFontSize(8f));
                    Cell driver;
                    try {
                        driver = new Cell().add(new Paragraph(report.getDriverName()).setFontSize(8f));
                    } catch (Exception ex) {
                        LOGGER.warn("Exception DRIVER: " + ex);
                        driver = new Cell().add(new Paragraph(""));
                    }
                    //add cells to table
                    table.addCell(startTime);
                    table.addCell(endTime);
                    table.addCell(distance);
                    table.addCell(eAddr);
                    table.addCell(driver);


                }
                document.add(table);
                document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
                count++;
            }
        }
        LOGGER.warn("closing...");
        //close pdf
        document.setMargins(0, 0, 0, 0);
        document.close();
    }

}
