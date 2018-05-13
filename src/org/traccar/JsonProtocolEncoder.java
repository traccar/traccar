/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.traccar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.traccar.model.Command;

/**
 *
 * @author Samson
 */
public abstract class JsonProtocolEncoder extends BaseProtocolEncoder {

    @Override
    protected Object encodeCommand(Command command) {
        Map<String, Object> allAttributes = new HashMap<>();
        allAttributes.put("id", command.getDeviceId());
        allAttributes.put("cmd", command.getType());
        Map<String, Object> attributes = command.getAttributes();
        allAttributes.putAll(attributes);
        Map<String, Object> extAttributes = getExtendedAttributes(command);
        if (extAttributes != null) {
            allAttributes.putAll(extAttributes);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(allAttributes);
            return json;
        } catch (JsonProcessingException ex) {
            Logger.getLogger(JsonProtocolEncoder.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    protected Map<String, Object> getExtendedAttributes(Command command) {
        return null;
    }
}
