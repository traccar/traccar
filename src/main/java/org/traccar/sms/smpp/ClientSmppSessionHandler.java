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
package org.traccar.sms.smpp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.util.SmppUtil;

public class ClientSmppSessionHandler extends DefaultSmppSessionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSmppSessionHandler.class);

    private SmppClient smppClient;

    public ClientSmppSessionHandler(SmppClient smppClient) {
        this.smppClient = smppClient;
    }

    @Override
    public void firePduRequestExpired(PduRequest pduRequest) {
        LOGGER.warn("PDU request expired: " + pduRequest);
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest request) {
        PduResponse response;
        try {
            if (request instanceof DeliverSm) {
                String sourceAddress = ((DeliverSm) request).getSourceAddress().getAddress();
                String message = CharsetUtil.decode(((DeliverSm) request).getShortMessage(),
                        smppClient.mapDataCodingToCharset(((DeliverSm) request).getDataCoding()));
                LOGGER.info("SMS Message Received: " + message.trim() + ", Source Address: " + sourceAddress);

                boolean isDeliveryReceipt;
                if (smppClient.getDetectDlrByOpts()) {
                    isDeliveryReceipt = request.getOptionalParameters() != null;
                } else {
                    isDeliveryReceipt = SmppUtil.isMessageTypeAnyDeliveryReceipt(((DeliverSm) request).getEsmClass());
                }

                if (!isDeliveryReceipt) {
                    TextMessageEventHandler.handleTextMessage(sourceAddress, message);
                }
            }
            response = request.createResponse();
        } catch (Exception error) {
            LOGGER.warn("SMS receiving error", error);
            response = request.createResponse();
            response.setResultMessage(error.getMessage());
            response.setCommandStatus(SmppConstants.STATUS_UNKNOWNERR);
        }
        return response;
    }

    @Override
    public void fireChannelUnexpectedlyClosed() {
        LOGGER.warn("SMPP session channel unexpectedly closed");
        smppClient.scheduleReconnect();
    }

}
