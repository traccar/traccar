package org.traccar.rest.reports;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.traccar.model.Position;
import org.traccar.rest.utils.SessionUtil;
import org.traccar.web.JsonConverter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by niko on 11/28/15.
 */
@Path("report")

public class PositionReportResource {

    @Context
    HttpServletResponse response;

    @javax.ws.rs.core.Context
    HttpServletRequest req;

    @GET
    @Path("csv")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getEventsAsCsv() throws Throwable {
        String separator = "\n";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"position.csv\"");
        ServletOutputStream outputStream = response.getOutputStream();

        StringWriter stringWriter = new StringWriter();
        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(separator);
        CSVPrinter csvFilePrinter = new CSVPrinter(stringWriter, csvFileFormat);

        long deviceId = Long.parseLong(req.getParameter("deviceId"));

        Collection<Position> positions = org.traccar.Context.getDataManager().getPositions(
                SessionUtil.getUserId(req), deviceId,
                JsonConverter.parseDate(req.getParameter("from")),
                JsonConverter.parseDate(req.getParameter("to")));

        for (Position position : positions) {
            List positionRecord = new ArrayList();
            positionRecord.add(String.valueOf(position.getValid()));
            positionRecord.add(position.getDeviceTime().toString());
            positionRecord.add(String.valueOf(position.getLatitude()));
            positionRecord.add(String.valueOf(position.getLongitude()));
            positionRecord.add(String.valueOf(position.getAltitude()));
            positionRecord.add(String.valueOf(position.getSpeed()));
            positionRecord.add(position.getAddress());
            csvFilePrinter.printRecord(positionRecord);
        }
        outputStream.write(stringWriter.toString().getBytes());
        return Response.ok().build();
    }
}
