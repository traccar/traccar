/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.traccar.api.resource;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.MediaType;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.Server;
import org.traccar.model.User;
import org.traccar.notification.NotificationFormatter;
import org.traccar.reports.ReportUtils;
import org.traccar.reports.Summary;
import org.traccar.reports.model.SummaryReport;

@Path("whatsapp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WhatsappResource {

    private static final List<String> commands = Arrays.asList("LOCALIZAR",
            "EMERGENCIA", "SEGUIR", "BLOQUEAR", "DESBLOQUEAR", "VELOCIDADE",
            "SILENCIAR", "NOTIFICAR", "SUPORTE", "CANCELAR", "KMHOJE", "KMONTEM",
            "KMSEMANA", "KMMES");
    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsappResource.class);

    @POST
    @PermitAll
    @SuppressWarnings("empty-statement")
    public String message(Map map) {
        try {
            if (map == null || map.get("from") == null || map.get("body") == null) {
                return NotificationFormatter.formatMessage(null, "defaultError", "whatsapp");
            }

            String from = map.get("from").toString().split("@")[0];
            Map mapParams = new HashMap();
            mapParams.put("from", from);

            Server server = Context.getDataManager().getServer();
            String support = server.getString("whatsappSupport");

            User user = Context.getDataManager().getUserByAttribute("whatsapp", from);
            if (user == null) {
                return NotificationFormatter.formatMessage(null, "userNotFound", "whatsapp");
            }

            if (user.getString("whatsappSupport") != null && !user.getString("whatsappSupport").isEmpty()) {
                support = server.getString("whatsappSupport");
            }

            mapParams.put("whatsappSupport", support);

            if (user.getDisabled()) {
                return NotificationFormatter.formatMessage(mapParams, "userDisabled", "whatsapp");
            }

            mapParams.put("user", user);

            List<String> messages = Arrays.asList(map.get("body").toString()
                    .split(" ")).stream().filter(s -> !s.trim().isEmpty())
                    .map((String s) -> {
                        String nfdNormalizedString = Normalizer.normalize(s, Normalizer.Form.NFD);
                        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
                        return pattern.matcher(nfdNormalizedString).replaceAll("").toUpperCase();
                    })
                    .collect(Collectors.toList());

            if (messages.size() <= 0) {
                return NotificationFormatter.formatMessage(null, "commandNotFound", "whatsapp");
            }

            if (!commands.contains(messages.get(0))) {
                return NotificationFormatter.formatMessage(mapParams, "menu", "whatsapp");
            }

            Set<Long> allUserItems = Context.getDeviceManager().getAllUserItems(user.getId());

            Collection<Device> devices = Context.getDeviceManager().getItems(allUserItems);
            if (devices == null || devices.isEmpty()) {
                return NotificationFormatter.formatMessage(null, "deviceNotFound", "whatsapp");
            }

            switch (messages.get(0)) {
                case "SUPORTE":
                    makeRequst(support, NotificationFormatter.formatMessage(mapParams, "supportForward", "whatsapp"));
                    return NotificationFormatter.formatMessage(mapParams, "supportResponse", "whatsapp");
                case "SILENCIAR":
                    if (messages.size() > 1 && messages.get(1) != null && messages.get(1).contains("SECUN")) {
                        mapParams.put("secondary", true);
                        user.set("whatsappSecondarySilent", Boolean.TRUE);
                    } else {
                        user.set("whatsappSilent", Boolean.TRUE);
                    }
                    Context.getUsersManager().updateItem(user);
                    return NotificationFormatter
                            .formatMessage(mapParams, "notify", "whatsapp");
                case "NOTIFICAR":
                    if (messages.size() > 1 && messages.get(1) != null && messages.get(1).contains("SECUN")) {
                        mapParams.put("secondary", true);
                        user.getAttributes().remove("whatsappSecondarySilent");
                    } else {
                        user.getAttributes().remove("whatsappSilent");
                    }
                    Context.getUsersManager().updateItem(user);
                    mapParams.put("notify", true);
                    return NotificationFormatter
                            .formatMessage(mapParams, "notify", "whatsapp");
            }

            Device device = null;
            if (devices.size() == 1) {
                device = devices.stream().findFirst().get();
            } else if (messages.size() > 1) {
                String name = "";
                if (messages.get(0).equalsIgnoreCase("VELOCIDADE") && messages.size() > 2) {
                    name = messages.get(2);
                } else {
                    name = messages.get(1);
                }

                for (Device d : devices) {
                    if (!d.getName().isEmpty() && d.getName().toUpperCase().contains(name)) {
                        device = d;
                    } else {
                        for (Map.Entry<String, Object> entry : d.getAttributes().entrySet()) {
                            Object v = entry.getValue();
                            if (v.toString().toUpperCase().contains(messages.get(1))) {
                                device = d;
                            }
                        }
                    }
                }
            }

            if (device == null) {
                return NotificationFormatter.formatMessage(mapParams, "deviceNotFound", "whatsapp");
            }

            Position position = Context.getDeviceManager().getLastPosition(device.getId());

            mapParams.put("device", device);
            mapParams.put("position", position);
            mapParams.put("timezone", ReportUtils.getTimezone(user.getId()));
            TimeZone timezone = ReportUtils.getTimezone(user.getId());
            mapParams.put("dateTool", new DateTool());
            mapParams.put("numberTool", new NumberTool());
            mapParams.put("locale", Locale.getDefault());

            String whatsappEmergency = user.getString("whatsappEmergency");

            ZoneId clientTimezone = ZoneId.of(timezone.getID());
            ZoneId serverTimezone = ZoneId.of(TimeZone.getDefault().getID());
            ZonedDateTime zonedDateTime = ZonedDateTime.of(LocalDate.now(clientTimezone)
                    .atStartOfDay(clientTimezone).toLocalDateTime(), clientTimezone)
                    .withZoneSameInstant(serverTimezone);

            Date startDate;
            Date endDate;
            Collection<SummaryReport> report;

            switch (messages.get(0)) {
                case "LOCALIZAR":
                    if (position == null) {
                        return NotificationFormatter.formatMessage(mapParams, "positionNotFound", "whatsapp");
                    }
                    return NotificationFormatter
                            .formatMessage(user.getId(), new Event(Event.TYPE_DEVICE_STATUS, device.getId(), position.getId()), position, "whatsapp");
                case "SEGUIR":
                    if (position == null) {
                        return NotificationFormatter.formatMessage(mapParams, "positionNotFound", "whatsapp");
                    }
                    return NotificationFormatter
                            .formatMessage(user.getId(), new Event(Event.TYPE_DEVICE_FOLLOW, device.getId(), position.getId()), position, "whatsapp");
                case "BLOQUEAR":
                    if (user.getLimitCommands()) {
                        return NotificationFormatter
                                .formatMessage(mapParams, "commandNotPermitted", "whatsapp");
                    }
                    Command commandStop = new Command();
                    commandStop.setType(Command.TYPE_ENGINE_STOP);
                    commandStop.setDeviceId(device.getId());
                    try {
                        Context.getCommandsManager().sendCommand(commandStop);
                        return NotificationFormatter
                                .formatMessage(mapParams, "engineStop", "whatsapp");

                    } catch (Exception ex) {
                        return NotificationFormatter
                                .formatMessage(mapParams, "commandNotSended", "whatsapp");
                    }
                case "DESBLOQUEAR":
                    if (user.getLimitCommands()) {
                        return NotificationFormatter
                                .formatMessage(mapParams, "commandNotPermitted", "whatsapp");
                    }
                    Command commandResume = new Command();
                    commandResume.setType(Command.TYPE_ENGINE_RESUME);
                    commandResume.setDeviceId(device.getId());
                    try {
                        Context.getCommandsManager().sendCommand(commandResume);
                        return NotificationFormatter
                                .formatMessage(mapParams, "engineResume", "whatsapp");
                    } catch (Exception ex) {
                        return NotificationFormatter
                                .formatMessage(mapParams, "commandNotSended", "whatsapp");
                    }
                case "VELOCIDADE":
                    if (messages.get(1) != null && !messages.get(1).isEmpty()) {
                        int speed;
                        try {
                            speed = Integer.parseInt(messages.get(1));
                            device.set("speedLimit", UnitsConverter.knotsFromKph(speed));
                            Context.getDeviceManager().updateItem(device);
                            mapParams.put("device", device);
                            mapParams.put("numberTool", new NumberTool());
                            return NotificationFormatter
                                    .formatMessage(mapParams, "speedLimit", "whatsapp");
                        } catch (NumberFormatException e) {
                        }
                    }
                case "EMERGENCIA":
                    makeRequst(whatsappEmergency, NotificationFormatter
                            .formatMessage(mapParams, "emergencyForward", "whatsapp"));

                    return NotificationFormatter
                            .formatMessage(mapParams, "emergencyResponse", "whatsapp");

                case "CANCELAR":
                    makeRequst(whatsappEmergency, NotificationFormatter
                            .formatMessage(mapParams, "cancelEmergencyForward", "whatsapp"));

                    return NotificationFormatter
                            .formatMessage(mapParams, "cancelEmergencyResponse", "whatsapp");

                case "KMHOJE":
                    startDate = Date.from(zonedDateTime.toInstant());
                    endDate = Date.from(zonedDateTime.plusHours(23).plusMinutes(59).plusSeconds(59).toInstant());
                    mapParams.put("startDate", startDate);
                    mapParams.put("endDate", endDate);

                    report = Summary.getObjects(user.getId(),
                            Arrays.asList(device.getId()),
                            new ArrayList<>(),
                            startDate,
                            endDate);
                    mapParams.put("report", report);
                    return NotificationFormatter
                            .formatMessage(mapParams, "report", "whatsapp");

                case "KMONTEM":
                    startDate = Date.from(zonedDateTime.minusDays(1).toInstant());
                    endDate = Date.from(zonedDateTime.minusDays(1).plusHours(23).plusMinutes(59).plusSeconds(59).toInstant());
                    mapParams.put("startDate", startDate);
                    mapParams.put("endDate", endDate);

                    report = Summary.getObjects(user.getId(),
                            Arrays.asList(device.getId()),
                            new ArrayList<>(),
                            startDate,
                            endDate);
                    mapParams.put("report", report);
                    return NotificationFormatter
                            .formatMessage(mapParams, "report", "whatsapp");

                case "KMSEMANA":
                    startDate = Date.from(zonedDateTime.with(ChronoField.DAY_OF_WEEK, 1).toInstant());
                    endDate = Date.from(zonedDateTime.with(ChronoField.DAY_OF_WEEK, 7).plusHours(23).plusMinutes(59).plusSeconds(59).toInstant());
                    mapParams.put("startDate", startDate);
                    mapParams.put("endDate", endDate);

                    report = Summary.getObjects(user.getId(),
                            Arrays.asList(device.getId()),
                            new ArrayList<>(),
                            startDate,
                            endDate);
                    mapParams.put("report", report);
                    return NotificationFormatter
                            .formatMessage(mapParams, "report", "whatsapp");

                case "KMMES":
                    startDate = Date.from(zonedDateTime.with(ChronoField.DAY_OF_MONTH, 1).toInstant());
                    endDate = Date.from(zonedDateTime.plusMonths(1).with(ChronoField.DAY_OF_MONTH, 1).minusDays(1).plusHours(23).plusMinutes(59).plusSeconds(59).toInstant());
                    mapParams.put("startDate", startDate);
                    mapParams.put("endDate", endDate);

                    report = Summary.getObjects(user.getId(),
                            Arrays.asList(device.getId()),
                            new ArrayList<>(),
                            startDate,
                            endDate);
                    mapParams.put("report", report);
                    return NotificationFormatter
                            .formatMessage(mapParams, "report", "whatsapp");

                default:
                    return NotificationFormatter.formatMessage(null, "menu", "whatsapp");
            }

        } catch (Exception ex) {
            return NotificationFormatter
                    .formatMessage(null, "defaultError", "whatsapp");
        }
    }

    private void makeRequst(String to, String msg) {
        Map map = new HashMap<>();
        to = to.concat("@c.us");
        map.put("to", to);
        map.put("msg", msg);

        String url = Context.getConfig().getString("notificator.whatsapp.url");
        Context.getClient().target(url).request()
                .async().post(Entity.json(map), new InvocationCallback<Object>() {
                    @Override
                    public void completed(Object o) {
                        System.out.println(o);
                    }

                    @Override
                    public void failed(Throwable throwable) {
                        LOGGER.warn("Whatsapp API error", throwable);
                    }
                });
    }
}
