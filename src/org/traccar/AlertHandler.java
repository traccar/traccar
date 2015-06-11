/*
 * Copyright 2015 alexis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar;

import java.util.Collection;
import java.util.List;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder; 
import org.traccar.model.Alert;
import org.traccar.model.Device;
import org.traccar.model.Event;
import org.traccar.model.Position;

/**
 *
 * @author alexis
 */
public class AlertHandler extends OneToOneDecoder {

    public AlertHandler() {

    }

    private void checkAlerts(Position p, Device d) throws Exception {
        Long idDevice = p.getDeviceId();
        Collection<Alert> alerts = Context.getDataManager().getAlertsByDevice(idDevice);
        if (alerts != null && !alerts.isEmpty()) {
            //int speed = (int) (p.getSpeed() * 1.852);
            for (Alert alerta : alerts) {
                
            }
        }
    }

    @Override
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        if (msg instanceof Position) {
            Position position = (Position) msg;

        } else if (msg instanceof List) {
            List<Position> positions = (List<Position>) msg;
            for (Position position : positions) {

            }
        }
        
        return msg;
    }
}
