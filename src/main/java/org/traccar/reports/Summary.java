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

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import org.jxls.util.JxlsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.model.Position;
import org.traccar.reports.model.SummaryReport;

public final class Summary {
    private static final Logger LOGGER = LoggerFactory.getLogger(Summary.class);
    private Summary() {
    }

    private static SummaryReport calculateSummaryResult(long deviceId, Date from, Date to) throws SQLException {
        SummaryReport result = new SummaryReport();
        result.setDeviceId(deviceId);
        result.setDeviceName(Context.getIdentityManager().getById(deviceId).getName());
        Collection<Position> positions = Context.getDataManager().getPositions(deviceId, from, to);
        if (positions != null && !positions.isEmpty()) {
            Position firstPosition = null;
            Position previousPosition = null;
            double speedSum = 0;
            boolean engineHoursEnabled = Context.getConfig().getBoolean("processing.engineHours.enable");
            for (Position position : positions) {
                if (firstPosition == null) {
                    firstPosition = position;
                }
                if (engineHoursEnabled && previousPosition != null
                        && position.getBoolean(Position.KEY_IGNITION)
                        && previousPosition.getBoolean(Position.KEY_IGNITION)) {
                    // Temporary fallback for old data, to be removed in May 2019
                    result.addEngineHours(position.getFixTime().getTime()
                            - previousPosition.getFixTime().getTime());
                }
                previousPosition = position;
                speedSum += position.getSpeed();
                result.setMaxSpeed(position.getSpeed());
            }
            boolean ignoreOdometer = Context.getDeviceManager()
                    .lookupAttributeBoolean(deviceId, "report.ignoreOdometer", false, false, true);
            result.setDistance(ReportUtils.calculateDistance(firstPosition, previousPosition, !ignoreOdometer));
            result.setAverageSpeed(speedSum / positions.size());
            result.setSpentFuel(ReportUtils.calculateFuel(firstPosition, previousPosition));

            if (engineHoursEnabled
                    && firstPosition.getAttributes().containsKey(Position.KEY_HOURS)
                    && previousPosition.getAttributes().containsKey(Position.KEY_HOURS)) {
                result.setEngineHours(
                        previousPosition.getLong(Position.KEY_HOURS) - firstPosition.getLong(Position.KEY_HOURS));
            }

            if (!ignoreOdometer
                    && firstPosition.getDouble(Position.KEY_ODOMETER) != 0
                    && previousPosition.getDouble(Position.KEY_ODOMETER) != 0) {
                result.setStartOdometer(firstPosition.getDouble(Position.KEY_ODOMETER));
                result.setEndOdometer(previousPosition.getDouble(Position.KEY_ODOMETER));
            } else {
                result.setStartOdometer(firstPosition.getDouble(Position.KEY_TOTAL_DISTANCE));
                result.setEndOdometer(previousPosition.getDouble(Position.KEY_TOTAL_DISTANCE));
            }

        }
        return result;
    }

    public static Collection<SummaryReport> getObjects(long userId, Collection<Long> deviceIds,
            Collection<Long> groupIds, Date from, Date to) throws SQLException {
        ReportUtils.checkPeriodLimit(from, to);
        ArrayList<SummaryReport> result = new ArrayList<>();
        for (long deviceId: ReportUtils.getDeviceList(deviceIds, groupIds)) {
            Context.getPermissionsManager().checkDevice(userId, deviceId);
            result.add(calculateSummaryResult(deviceId, from, to));
        }
        return result;
    }

    public static void getExcel(OutputStream outputStream,
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws SQLException, IOException {
        ReportUtils.checkPeriodLimit(from, to);
        Collection<SummaryReport> summaries = getObjects(userId, deviceIds, groupIds, from, to);
        String templatePath = Context.getConfig().getString("report.templatesPath",
                "templates/export/");
        try (InputStream inputStream = new FileInputStream(templatePath + "/summary.xlsx")) {
            org.jxls.common.Context jxlsContext = ReportUtils.initializeContext(userId);
            jxlsContext.putVar("summaries", summaries);
            jxlsContext.putVar("from", from);
            jxlsContext.putVar("to", to);
            JxlsHelper.getInstance().setUseFastFormulaProcessor(false)
                    .processTemplate(inputStream, outputStream, jxlsContext);
        }
    }

    public static void getPdf(OutputStream outputStream,
                              long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
                              Date from, Date to) throws SQLException, IOException {
        ReportUtils.checkPeriodLimit(from, to);
        Collection<SummaryReport> summaries = getObjects(userId, deviceIds, groupIds, from, to);
        PdfDocument pdf = new PdfDocument(new PdfWriter(outputStream));
        Document document = new Document(pdf, PageSize.A4);
        document.getPdfDocument().setDefaultPageSize(PageSize.A4.rotate());
        pdf.addEventHandler(PdfDocumentEvent.START_PAGE,
                new ReportUtils.TextFooterEventHandler(document, userId));

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        //title
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
        SimpleDateFormat repDate = new SimpleDateFormat("yyyy-MM-dd");
        Text title =
                new Text("Report Type: Summary").setFont(font).setFontSize(16f);
        Text dates = new Text("Period: "+ repDate.format(from) +
                " to " + repDate.format(to)).setFont(font).setFontSize(14f);
        Paragraph tit = new Paragraph().add(title);
        Paragraph date = new Paragraph().add(dates);
        document.add(tit);
        document.add(date);
        //body
        LOGGER.warn("Creating pdf...");
        Table table = new Table(5).useAllAvailableWidth();
        Cell deviceDate = new Cell().add(new Paragraph("Date"))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceDevice = new Cell().add(new Paragraph("Device"))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceDistance = new Cell().add(new Paragraph("Distance"))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceTopSpeed = new Cell().add(new Paragraph("Top Speed"))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        Cell deviceDriver = new Cell().add(new Paragraph("Driver"))
                .setBackgroundColor(ColorConstants.BLUE)
                .setFontColor(ColorConstants.WHITE);
        table.addCell(deviceDate);
        table.addCell(deviceDevice);
        table.addCell(deviceDistance);
        table.addCell(deviceTopSpeed);
        table.addCell(deviceDriver);
        for(SummaryReport report: summaries){
            LOGGER.warn("populating table...");
            Cell sumdate = new Cell().add(new Paragraph(repDate.format(report.getSumDate())));
            Cell device = new Cell().add(new Paragraph(report.getDeviceName()));
            Cell distance = new Cell().add(new Paragraph(String.format("%.2f",
                    report.getDistance()*0.001)+"km"));
            Cell topSpeed = new Cell().add(new Paragraph(Math.round(report
                    .getMaxSpeed()*1.852)+"km/h"));
            Cell driver;
            try {
                driver = new Cell().add(new Paragraph(report.getDriverName()));
            }catch (Exception ex){
                LOGGER.warn("Exception: "+ex);
                driver = new Cell().add(new Paragraph(""));
            }
            //add cells to table
            table.addCell(sumdate);
            table.addCell(device);
            table.addCell(distance);
            table.addCell(topSpeed);
            table.addCell(driver);

        }
        document.add(table);
        LOGGER.warn("closing...");
        //close pdf
        document.close();
    }
}
