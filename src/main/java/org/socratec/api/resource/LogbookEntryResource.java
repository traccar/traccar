package org.socratec.api.resource;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traccar.api.SimpleObjectResource;
import org.traccar.helper.LogAction;
import org.traccar.helper.model.DeviceUtil;
import org.traccar.model.Device;
import org.traccar.model.UserRestrictions;
import org.traccar.reports.common.ReportUtils;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;
import org.socratec.model.LogbookEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Path("logbook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LogbookEntryResource extends SimpleObjectResource<LogbookEntry> {

    @Inject
    private ReportUtils reportUtils;

    @Inject
    private Storage storage;

    @Inject
    private LogAction actionLogger;

    @Context
    private HttpServletRequest request;

    public LogbookEntryResource() {
        super(LogbookEntry.class, "id");
    }

    @Override
    @GET
    public Collection<LogbookEntry> get(
            @QueryParam("all") boolean all,
            @QueryParam("userId") long userId) throws StorageException {

        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        return super.get(all, userId);
    }

    @GET
    @Path("report")
    public Collection<LogbookEntry> getReport(
            @QueryParam("deviceId") List<Long> deviceIds,
            @QueryParam("groupId") List<Long> groupIds,
            @QueryParam("from") Date from,
            @QueryParam("to") Date to) throws StorageException {

        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);

        if (from == null || to == null) {
            throw new IllegalArgumentException("Both 'from' and 'to' parameters are required for filtered queries");
        }

        actionLogger.report(request, getUserId(), false, "logbook", from, to, deviceIds, groupIds);
        return getFilteredLogbookEntries(getUserId(), deviceIds, groupIds, from, to);
    }

    @Override
    @PUT
    @Path("{id}")
    public Response update(LogbookEntry entity) throws Exception {
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);
        permissionsService.checkPermission(LogbookEntry.class, getUserId(), entity.getId());

        // Get existing entry from database
        LogbookEntry existing = storage.getObject(LogbookEntry.class, new Request(
                new Columns.All(), new Condition.Equals("id", entity.getId())));

        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Update ONLY the type field - all other fields are ignored
        existing.setType(entity.getType());

        // Save the updated entry
        storage.updateObject(existing, new Request(
                new Columns.Include("type"),
                new Condition.Equals("id", entity.getId())));

        // Log the update action
        actionLogger.edit(request, getUserId(), existing);

        return Response.ok(existing).build();
    }

    /**
     * Private method containing the merged logic from LogbookReportProvider
     * Handles filtered queries with date range and device/group filtering
     */
    private Collection<LogbookEntry> getFilteredLogbookEntries(
            long userId, Collection<Long> deviceIds, Collection<Long> groupIds,
            Date from, Date to) throws StorageException {

        reportUtils.checkPeriodLimit(from, to);

        ArrayList<LogbookEntry> result = new ArrayList<>();
        for (Device device : DeviceUtil.getAccessibleDevices(storage, userId, deviceIds, groupIds)) {
            Collection<LogbookEntry> logbookEntries = storage.getObjects(LogbookEntry.class, new Request(
                    new Columns.All(),
                    new Condition.And(
                            new Condition.Equals("deviceId", device.getId()),
                            new Condition.Between("startTime", "from", from, "to", to)
                    ),
                    new Order("startTime")
            ));
            result.addAll(logbookEntries);
        }
        return result;
    }
}
