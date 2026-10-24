/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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
package org.traccar.api.resource;

import org.traccar.BasePipelineFactory;
import org.traccar.api.BaseResource;
import org.traccar.helper.LogAction;
import org.traccar.media.VideoRecordingFilter;
import org.traccar.media.VideoRecordingManager;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.UserRestrictions;
import org.traccar.protocol.Jt808ProtocolDecoder;
import org.traccar.session.ConnectionManager;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.DateTimeException;
import java.util.UUID;

@Path("video/{deviceId}/recordings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VideoRecordingResource extends BaseResource {

    private final VideoRecordingManager recordingManager;
    private final ConnectionManager connectionManager;
    private final LogAction actionLogger;

    @Context
    private HttpServletRequest request;

    @Inject
    public VideoRecordingResource(
            VideoRecordingManager recordingManager, ConnectionManager connectionManager, LogAction actionLogger) {
        this.recordingManager = recordingManager;
        this.connectionManager = connectionManager;
        this.actionLogger = actionLogger;
    }

    @POST
    public Response create(@PathParam("deviceId") long deviceId, VideoRecordingFilter filter) throws Exception {
        permissionsService.checkPermission(Device.class, getUserId(), deviceId);
        permissionsService.checkRestriction(getUserId(), UserRestrictions::getLimitCommands);
        if (filter == null) {
            throw new BadRequestException("Recording filter is required");
        }
        try {
            filter.validate();
        } catch (IllegalArgumentException | DateTimeException error) {
            throw new BadRequestException("Invalid recording filter", error);
        }
        var session = connectionManager.getDeviceSession(deviceId);
        if (session == null || !session.supportsLiveCommands()
                || BasePipelineFactory.getHandler(
                        session.getChannel().pipeline(), Jt808ProtocolDecoder.class) == null) {
            throw new WebApplicationException("A JT808 device connected to this server is required", 409);
        }
        VideoRecordingManager.Request query;
        try {
            query = recordingManager.create(deviceId, getUserId());
        } catch (IllegalStateException error) {
            throw new WebApplicationException(error.getMessage(), 409);
        }
        Command command = new Command();
        command.setDeviceId(deviceId);
        command.setType(Command.TYPE_VIDEO_RECORDING_LIST);
        command.set(Command.KEY_NO_QUEUE, true);
        command.set("from", filter.from());
        command.set("to", filter.to());
        command.set(Command.KEY_INDEX, filter.channel());
        command.set("resourceType", filter.resourceType());
        command.set("streamType", filter.streamType());
        command.set("storageType", filter.storageType());
        command.set("alarmMask", filter.mask());
        command.set("sequence", query.sequence());
        try {
            session.sendCommand(command);
            actionLogger.command(request, getUserId(), 0, deviceId, command.getType());
        } catch (RuntimeException error) {
            recordingManager.fail(deviceId, query.sequence(), "Failed to send recording query");
            throw new WebApplicationException("Failed to send recording query", 502);
        }
        return Response.accepted(recordingManager.get(deviceId, getUserId(), query.id())).build();
    }

    @GET
    @Path("{requestId}")
    public VideoRecordingManager.Result get(
            @PathParam("deviceId") long deviceId, @PathParam("requestId") UUID requestId) throws Exception {
        permissionsService.checkPermission(Device.class, getUserId(), deviceId);
        var result = recordingManager.get(deviceId, getUserId(), requestId);
        if (result == null) {
            throw new NotFoundException();
        }
        return result;
    }
}
