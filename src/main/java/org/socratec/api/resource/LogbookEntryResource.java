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
import org.socratec.model.LogbookEntry;
import org.traccar.api.BaseObjectResource;
import org.traccar.helper.LogAction;
import org.traccar.helper.model.DeviceUtil;
import org.traccar.model.Device;
import org.traccar.model.User;
import org.traccar.model.UserRestrictions;
import org.traccar.reports.common.ReportUtils;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Order;
import org.traccar.storage.query.Request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Path("logbook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LogbookEntryResource extends BaseObjectResource<LogbookEntry> {

    @Inject
    private ReportUtils reportUtils;

    @Inject
    private Storage storage;

    @Inject
    private LogAction actionLogger;

    @Context
    private HttpServletRequest request;

    public LogbookEntryResource() {
        super(LogbookEntry.class);
    }

    @GET
    public Collection<LogbookEntry> get(
            @QueryParam("all") boolean all,
            @QueryParam("userId") long userId) throws StorageException {

        permissionsService.checkRestriction(getUserId(), UserRestrictions::getDisableReports);

        // Custom permission logic based on device access instead of direct user-logbook permissions
        return getLogbookEntriesForUser(getUserId(), all, userId);
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

        // Get existing entry from database first to check device permission
        LogbookEntry existing = storage.getObject(LogbookEntry.class, new Request(
                new Columns.All(), new Condition.Equals("id", entity.getId())));

        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Check if user has access to the device associated with this logbook entry
        permissionsService.checkPermission(Device.class, getUserId(), existing.getDeviceId());

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
     * Get LogbookEntry objects for a user based on device permissions
     * Uses an efficient database query with IN condition for accessible device IDs
     */
    private Collection<LogbookEntry> getLogbookEntriesForUser(
            long currentUserId,
            boolean all,
            long userId
    ) throws StorageException {
        if (userId == 0) {
            userId = currentUserId;
        } else {
            permissionsService.checkUser(currentUserId, userId);
        }

        // First, get all device IDs the user has access to using a direct permission query
        Collection<Device> accessibleDevices = storage.getObjects(Device.class, new Request(
                new Columns.Include("id"),
                new Condition.Permission(User.class, userId, Device.class)
        ));

        // Extract device IDs
        List<Long> accessibleDeviceIds = accessibleDevices.stream()
                .map(Device::getId)
                .collect(Collectors.toList());

        // If no accessible devices, return empty collection
        if (accessibleDeviceIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Get all logbook entries first
        var logbookEntries = storage.getObjects(LogbookEntry.class, new Request(
                new Columns.All(),
                new Order("startTime")
        ));

        // Convert accessibleDeviceIds to Set for efficient lookup
        var accessibleDeviceIdSet = new HashSet<>(accessibleDeviceIds);

        // Filter logbook entries to only include those with accessible device IDs
        return logbookEntries.stream()
                .filter(entry -> accessibleDeviceIdSet.contains(entry.getDeviceId()))
                .collect(Collectors.toList());
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
