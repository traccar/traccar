/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.traccar.protocol;

import org.traccar.JsonProtocolEncoder;
import org.traccar.model.Command;

/**
 *
 * @author Samson
 */
public class StrongTowerProtocolEncoder extends JsonProtocolEncoder {

    @Override
    protected Object encodeCommand(Command command) {
        String json = (String) super.encodeCommand(command);
        if (json != null) {
            return json + "\r\n";
        }
        return null;
    }
}
