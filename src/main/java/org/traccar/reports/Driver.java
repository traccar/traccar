package org.traccar.reports;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.reports.model.DriverReport;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

public class Driver {
    private static final Logger LOGGER = LoggerFactory.getLogger(Driver.class);

    public Driver(){

    }

    private static Collection<Position> getPositions(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
                                                     Date from, Date to) throws SQLException {
        ReportUtils.checkPeriodLimit(from, to);
        ArrayList<Position> result = new ArrayList<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            result.addAll(Context.getDataManager().getPositions(deviceId, from, to));
        }
        return result;
    }

    private static Collection<DriverReport> getDriversFromPositions(Collection<Position> positions){
        ArrayList<DriverReport> drivers = new ArrayList<>();
        if(!positions.isEmpty()) {
            for (Position position : positions) {
                DriverReport driver = new DriverReport();
                driver.setDeviceId(position.getDeviceId());
                try {
                    driver.setDriverUniqueId(position.getDriveruniqueid());
                } catch (NullPointerException ex) {
                    LOGGER.warn("Position without driver!");
                    driver.setDriverUniqueId(null);
                }
                driver.setReportDate(position.getFixTime());
                drivers.add(driver);
            }
        }else{
            LOGGER.warn("No drivers found!");
        }
        LOGGER.warn("Drivers: "+drivers);
        return drivers;
    }

    public static Collection<DriverReport> getObjects(long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
                                                      Date from, Date to) throws SQLException {
        SimpleDateFormat repDate = new SimpleDateFormat("yyyy-MM-dd");
        LOGGER.warn("Getting drivers......");
        Collection<Position> positions = getPositions(userId, deviceIds, groupIds, from, to);
        Collection<Position> trimmed = new ArrayList<>();
        Position prev = null;
        for(Position position: positions){
            if(prev != null){
                if(position.getDriveruniqueid() != null &&
                        !repDate.format(position.getFixTime()).equals(
                                repDate.format(prev.getFixTime()))){
                    trimmed.add(position);
                }
                prev = position;
            }else{
                if(position.getDriveruniqueid() != null)
                    trimmed.add(position);
                prev = position;
            }
        }
        return getDriversFromPositions(trimmed);
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
        SimpleDateFormat repDate = new SimpleDateFormat("yyyy-MM-dd");
        LOGGER.warn("Creating pdf...");
        for(long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Device device = Context.getIdentityManager().getById(deviceId);
            Text title =
                    new Text("Report Type: Drivers").setFont(font).setFontSize(12f);
            Text name =
                    new Text("Device: "+device.getName()).setFont(font).setFontSize(12f);
            Text dates = new Text("Period: "+ repDate.format(from) +
                    " to " + repDate.format(to)).setFont(font).setFontSize(12f);
            Paragraph tit = new Paragraph().add(title);
            Paragraph date = new Paragraph().add(dates);
            Paragraph nam = new Paragraph().add(name);
            document.add(tit);
            document.add(nam);
            document.add(date);
            //drivers during the period
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            Collection<Position> positions = Context.getDataManager().getPositions(deviceId, from, to);
            Collection<Position> trimmed = new ArrayList<>();
            Position prevRep = null;
            for(Position position: positions){
                if(prevRep != null){
                    if(position.getDriveruniqueid() != null &&
                            !repDate.format(position.getFixTime()).equals(
                                    repDate.format(prevRep.getFixTime()))){
                        trimmed.add(position);
                    }
                    prevRep = position;
                }else{
                    if(position.getDriveruniqueid() != null)
                        trimmed.add(position);
                    prevRep = position;
                }
            }
            Collection<DriverReport> drivers = getDriversFromPositions(trimmed);
            /*separate drivers by driver
            Collection<Collection<DriverReport>> driverDate = new ArrayList<>();
            DriverReport prev = null;
            //stores different drivers
            ArrayList<String> diffDrivers = new ArrayList<>();
            //arraylist to be added to diffDrivers
            Collection<DriverReport> driverCollection = new ArrayList<>();

            Iterator<DriverReport> tripIter= drivers.iterator();
            while(tripIter.hasNext()){
                DriverReport driverReport = tripIter.next();
                if(prev != null) {
                    if (driverReport.getDriveruniqueid() != null &&
                        prev.getDriveruniqueid() != null &&
                            !prev.getDriveruniqueid().equals(driverReport.getDriveruniqueid())) {
                        driverDate.add(driverCollection);
                        driverCollection = new ArrayList<>();
                        diffDrivers.add(driverReport.getDriveruniqueid());
                    }
                    prev = driverReport;
                    driverCollection.add(driverReport);
                    if(!drivers.iterator().hasNext()){
                        driverDate.add(driverCollection);
                    }
                }else{
                    diffDrivers.add(driverReport.getDriveruniqueid());
                    prev = driverReport;
                    driverCollection.add(driverReport);
                }
                if(!tripIter.hasNext()){
                    driverDate.add(driverCollection);
                }
            }*/

            float[] colWidths = {1,1};
            int count = 0;
            Table table = new Table(UnitValue.createPercentArray(colWidths))
                    .useAllAvailableWidth();

            createTableHeaders(table);
            for(DriverReport report: drivers){
                Cell dateR = new Cell().add(new Paragraph(repDate.
                        format(report.getReportDate())).setFontSize(8f));

                Cell vehicle = new Cell().add(new Paragraph(report.getDriverUniqueId())
                        .setFontSize(8f));

                table.addCell(dateR);
                table.addCell(vehicle);
            }
            document.add(table);
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        }
        LOGGER.warn("closing...");
        //close pdf
        document.setMargins(0,0,0,0);
        document.close();
    }

    private static void createTableHeaders(Table table) {
        Cell deviceSTm = new Cell().add(new Paragraph("Date").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceSAddr = new Cell().add(new Paragraph("Driver").setFontSize(10f))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);


        table.addHeaderCell(deviceSTm);
        table.addHeaderCell(deviceSAddr);
    }
}
