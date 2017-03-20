/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
 * Copyright 2017 Andrey Kunitsyn (andrey@traccar.org)
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
package org.traccar.smpp;

import org.traccar.events.TextMessageEventHandler;
import org.traccar.helper.Log;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;

public class ClientSmppSessionHandler extends DefaultSmppSessionHandler {

    private SmppClient smppClient;

    public ClientSmppSessionHandler(SmppClient smppClient) {
        this.smppClient = smppClient;
    }

    @Override
    public void firePduRequestExpired(PduRequest pduRequest) {
        Log.warning("PDU request expired: " + pduRequest);
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest request) {
        PduResponse response = null;
        try {
            if (request instanceof DeliverSm) {
                if (request.getOptionalParameters() != null) {
                    Log.debug("SMS Message Delivered: "
                            + request.getOptionalParameter(SmppConstants.TAG_RECEIPTED_MSG_ID).getValueAsString()
                            + ", State: "
                            + request.getOptionalParameter(SmppConstants.TAG_MSG_STATE).getValueAsByte());
                } else {
                    String sourceAddress = ((DeliverSm) request).getSourceAddress().getAddress();
                    String message = CharsetUtil.decode(((DeliverSm) request).getShortMessage(),
                            smppClient.mapDataCodingToCharset(((DeliverSm) request).getDataCoding()));
                    Log.debug("SMS Message Received: " + message.trim() + ", Source Address: " + sourceAddress);
                    TextMessageEventHandler.handleTextMessage(sourceAddress, message);
                }
            }
            response = request.createResponse();
        } catch (Throwable error) {
            Log.warning(error);
            response = request.createResponse();
            response.setResultMessage(error.getMessage());
            response.setCommandStatus(SmppConstants.STATUS_UNKNOWNERR);
        }
        return response;
    }

    @Override
    public void fireChannelUnexpectedlyClosed() {
        Log.warning("SMPP session channel unexpectedly closed");
        smppClient.scheduleReconnect();
    }
}
