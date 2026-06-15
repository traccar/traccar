package org.traccar.api.resource;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.traccar.api.BaseResource;
import org.traccar.database.StatisticsManager;
import org.traccar.model.Statistics;

@Path("metrics")
@Produces(MediaType.TEXT_PLAIN)
public class MetricsResource extends BaseResource {

    @Inject
    private StatisticsManager statisticsManager;

    @PermitAll
    @GET
    public String get() {
        Statistics statistics = statisticsManager.getStatistics();
        StringBuilder sb = new StringBuilder();
        sb.append("# TYPE traccar_requests_total counter\n");
        sb.append("traccar_requests_total ").append(statistics.getRequests()).append('\n');
        sb.append("# TYPE traccar_messages_received_total counter\n");
        sb.append("traccar_messages_received_total ").append(statistics.getMessagesReceived()).append('\n');
        sb.append("# TYPE traccar_messages_stored_total counter\n");
        sb.append("traccar_messages_stored_total ").append(statistics.getMessagesStored()).append('\n');
        sb.append("# TYPE traccar_mail_sent_total counter\n");
        sb.append("traccar_mail_sent_total ").append(statistics.getMailSent()).append('\n');
        sb.append("# TYPE traccar_sms_sent_total counter\n");
        sb.append("traccar_sms_sent_total ").append(statistics.getSmsSent()).append('\n');
        sb.append("# TYPE traccar_geocoder_requests_total counter\n");
        sb.append("traccar_geocoder_requests_total ").append(statistics.getGeocoderRequests()).append('\n');
        sb.append("# TYPE traccar_geolocation_requests_total counter\n");
        sb.append("traccar_geolocation_requests_total ").append(statistics.getGeolocationRequests()).append('\n');
        sb.append("# TYPE traccar_active_users gauge\n");
        sb.append("traccar_active_users ").append(statistics.getActiveUsers()).append('\n');
        sb.append("# TYPE traccar_active_devices gauge\n");
        sb.append("traccar_active_devices ").append(statistics.getActiveDevices()).append('\n');
        if (statistics.getProtocols() != null) {
            sb.append("# TYPE traccar_protocol_devices gauge\n");
            for (var entry : statistics.getProtocols().entrySet()) {
                sb.append("traccar_protocol_devices{protocol=\"")
                        .append(entry.getKey())
                        .append("\"} ")
                        .append(entry.getValue())
                        .append('\n');
            }
        }
        return sb.toString();
    }
}
