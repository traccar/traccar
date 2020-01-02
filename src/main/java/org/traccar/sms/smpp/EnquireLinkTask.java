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

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

public class EnquireLinkTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnquireLinkTask.class);

    private SmppClient smppClient;
    private Integer enquireLinkTimeout;

    public EnquireLinkTask(SmppClient smppClient, Integer enquireLinkTimeout) {
        this.smppClient = smppClient;
        this.enquireLinkTimeout = enquireLinkTimeout;
    }

    @Override
    public void run() {
        SmppSession smppSession = smppClient.getSession();
        if (smppSession != null && smppSession.isBound()) {
            try {
                smppSession.enquireLink(new EnquireLink(), enquireLinkTimeout);
            } catch (SmppTimeoutException | SmppChannelException
                    | RecoverablePduException | UnrecoverablePduException error) {
                LOGGER.warn("Enquire link failed, executing reconnect: ", error);
                smppClient.scheduleReconnect();
            } catch (InterruptedException error) {
                LOGGER.info("Enquire link interrupted, probably killed by reconnecting");
            }
        } else {
            LOGGER.warn("Enquire link running while session is not connected");
        }
    }

}
