package org.socratec.api.resource;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.socratec.model.LogbookEntry;
import org.socratec.model.LogbookEntryResult;
import org.socratec.model.LogbookEntriesResult;
import org.socratec.model.LogbookEntryType;
import org.traccar.api.BaseObjectResource;
import org.traccar.api.security.ServiceAccountUser;
import org.traccar.helper.LogAction;
import org.traccar.model.Device;
import org.traccar.model.UserRestrictions;
import org.traccar.reports.common.ReportUtils;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

import java.util.List;
import java.util.ArrayList;

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

    @Override
    @Path("{id}")
    @GET
    public Response getSingle(@PathParam("id") long id) throws StorageException {
        return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }

    @POST
    public Response add(LogbookEntry entry) throws Exception {
        // Restrict to service users only
        if (getUserId() != ServiceAccountUser.ID) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\": \"Access denied: Service account required\"}")
                    .build();
        }

        // Input validation
        if (entry == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Entry cannot be null.\"}")
                    .build();
        }

        try {
            validateCreateInput(entry);
            permissionsService.checkPermission(Device.class, getUserId(), entry.getDeviceId());

            processEntryDefaults(entry);
            entry.setId(storage.addObject(entry, new Request(new Columns.Exclude("id"))));

            actionLogger.create(request, getUserId(), entry);

            return Response.ok(entry).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("bulk")
    public Response addBulk(List<LogbookEntry> entries) throws Exception {
        // Restrict to service users only
        if (getUserId() != ServiceAccountUser.ID) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\": \"Access denied: Service account required\"}")
                    .build();
        }

        // Input validation
        if (entries == null || entries.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Entry list cannot be null or empty.\"}")
                    .build();
        }

        List<LogbookEntryResult> results = new ArrayList<>();
        int successCount = 0;

        // Process each entry individually
        for (int i = 0; i < entries.size(); i++) {
            LogbookEntry entry = entries.get(i);
            try {
                validateCreateInput(entry);
                permissionsService.checkPermission(Device.class, getUserId(), entry.getDeviceId());

                processEntryDefaults(entry);
                entry.setId(storage.addObject(entry, new Request(new Columns.Exclude("id"))));

                actionLogger.create(request, getUserId(), entry);
                results.add(LogbookEntryResult.success(i, entry));
                successCount++;
            } catch (Exception e) {
                // Add error result
                results.add(LogbookEntryResult.error(i, e.getMessage(), entry));
            }
        }

        return buildBulkResponse(results, successCount, entries.size());
    }

    @Override
    @Path("{id}")
    @DELETE
    public Response remove(@PathParam("id") long id) throws Exception {
        return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
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

        // Update ONLY the type and notes fields - all other fields are ignored
        existing.setType(entity.getType());
        existing.setNotes(entity.getNotes());

        // Save the updated entry
        storage.updateObject(existing, new Request(
                new Columns.Include("type", "notes"),
                new Condition.Equals("id", entity.getId())));

        // Log the update action
        actionLogger.edit(request, getUserId(), existing);

        return Response.ok(existing).build();
    }

    private void validateCreateInput(LogbookEntry entity) throws StorageException {
        if (entity.getHash() == null) {
            throw new IllegalArgumentException("Missing required hash.");
        }

        if (entity.getDeviceId() <= 0) {
            throw new IllegalArgumentException("Missing required deviceId.");
        }

        if (entity.getDriverId() <= 0) {
            throw new IllegalArgumentException("Missing required driverId.");
        }

        if (entity.getStartPositionId() <= 0 || entity.getEndPositionId() <= 0) {
            throw new IllegalArgumentException("Missing startPositionId and/or endPositionId.");
        }

        if (entity.getStartTime() == null || entity.getEndTime() == null) {
            throw new IllegalArgumentException("Missing startTime and/or endTime.");
        }

        if (entity.getEndTime().before(entity.getStartTime())) {
            throw new IllegalArgumentException("EndTime must be after startTime.");
        }
    }

    private void processEntryDefaults(LogbookEntry entity) {
        // Calculate duration if not provided
        if (entity.getDuration() <= 0 && entity.getStartTime() != null && entity.getEndTime() != null) {
            entity.setDuration(entity.getEndTime().getTime() - entity.getStartTime().getTime());
        }

        // Set default type if not provided
        if (entity.getType() == null) {
            entity.setType(LogbookEntryType.BUSINESS);
        }
    }

    private Response buildBulkResponse(List<LogbookEntryResult> results, int successCount, int totalCount) {
        LogbookEntriesResult response = new LogbookEntriesResult(
                totalCount,
                successCount,
                totalCount - successCount,
                results
        );

        if (successCount == totalCount) {
            // All succeeded - return 200 OK
            return Response.ok(response).build();
        } else if (successCount > 0) {
            // Partial success - return 207 Multi-Status
            return Response.status(207).entity(response).build();
        } else {
            // All failed - return 400 Bad Request
            return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
        }
    }
}
