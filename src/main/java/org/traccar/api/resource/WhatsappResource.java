/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.traccar.api.resource;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.velocity.tools.generic.NumberTool;
import org.traccar.Context;
import org.traccar.helper.UnitsConverter;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;
import org.traccar.model.User;
import org.traccar.notification.NotificationFormatter;

@Path("whatsapp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WhatsappResource {

    final private List<String> commands = Arrays.asList("LOCALIZAR", "SEGUIR", "BLOQUEAR", "DESBLOQUEAR", "VELOCIDADE", "SILENCIAR", "NOTIFICAR", "SUPORTE");

    @POST
    @PermitAll
    @SuppressWarnings("empty-statement")
    public String message(Map map) {
        try {
            if (map == null || map.get("from") == null || map.get("body") == null) {
                return "*Atenção*\nParametros incorretos de sistema. Entre em contato com o administrador.";
            }

            String from = map.get("from").toString().split("@")[0];

            User user = Context.getDataManager().getUserByAttribute("whatsapp", from);
            if (user == null) {
                return "*Atenção!*\nNenhum usuário cadastrado com esse número.";
            }

            List<String> messages = Arrays.asList(map.get("body").toString()
                    .split(" ")).stream().filter(s -> !s.trim().isEmpty())
                    .map(s -> s.toUpperCase())
                    .collect(Collectors.toList());

            if (messages.size() <= 0) {
                return "*Nenhum comando informado*\n Entre em contato com o administrador do sistema.";
            }

            if (!commands.contains(messages.get(0))) {
                return NotificationFormatter.formatMessage(null, "menu", "whatsapp");
            }

            Set<Long> allUserItems = Context.getDeviceManager().getAllUserItems(user.getId());

            Collection<Device> devices = Context.getDeviceManager().getItems(allUserItems);
            if (devices == null || devices.isEmpty()) {
                return "Nenhum rastreador cadastrado";
            }

            Map mapParams = new HashMap();

            switch (messages.get(0)) {
                case "SUPORTE":
                    return "SUPORTE";
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
                return "Nenhum rastreador encontrado para esse usuário.";
            }

            Position position = Context.getDeviceManager().getLastPosition(device.getId());

            mapParams.put("device", device);
            mapParams.put("user", user);

            switch (messages.get(0)) {
                case "LOCALIZAR":
                    if (position == null) {
                        return "Nenhuma posição para esse rastreador";
                    }
                    return NotificationFormatter
                            .formatMessage(user.getId(), new Event(Event.TYPE_DEVICE_STATUS, device.getId(), position.getId()), position, "whatsapp");
                case "SEGUIR":
                    if (position == null) {
                        return "Nenhuma posição para esse rastreador";
                    }
                    return NotificationFormatter
                            .formatMessage(user.getId(), new Event(Event.TYPE_DEVICE_FOLLOW, device.getId(), position.getId()), position, "whatsapp");
                case "BLOQUEAR":
                    Command commandStop = new Command();
                    commandStop.setType(Command.TYPE_ENGINE_STOP);
                    commandStop.setDeviceId(device.getId());
                     {
                        try {
                            boolean sended = Context.getCommandsManager().sendCommand(commandStop);
                            if (sended) {
                                return NotificationFormatter
                                        .formatMessage(mapParams, "engineStop", "whatsapp");
                            } else {
                                return "Não foi possível enviar o comando no momento. \nTente mais tarde!";
                            }
                        } catch (Exception ex) {
                            return "Erro ao enviar comando";
                        }
                    }
                case "DESBLOQUEAR":
                    Command commandResume = new Command();
                    commandResume.setType(Command.TYPE_ENGINE_RESUME);
                    commandResume.setDeviceId(device.getId());
                     {
                        try {
                            boolean sended = Context.getCommandsManager().sendCommand(commandResume);
                            if (sended) {
                                return NotificationFormatter
                                        .formatMessage(mapParams, "engineResume", "whatsapp");
                            } else {
                                return "Não foi possível enviar o comando no momento. \nTente mais tarde!";
                            }
                        } catch (Exception ex) {
                            return "Erro ao enviar comando";
                        }
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
                default:
                    return NotificationFormatter.formatMessage(null, "menu", "whatsapp");
            }

        } catch (Exception ex) {
            return "Erro ao acessar dados";
        }
    }
}
