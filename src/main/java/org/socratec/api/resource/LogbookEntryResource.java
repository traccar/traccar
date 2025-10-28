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
import org.traccar.api.BaseObjectResource;
import org.traccar.helper.LogAction;
import org.traccar.model.Device;
import org.traccar.model.UserRestrictions;
import org.traccar.reports.common.ReportUtils;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

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

    @Override
    @POST
    public Response add(LogbookEntry entity) throws Exception {
        return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
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
}
