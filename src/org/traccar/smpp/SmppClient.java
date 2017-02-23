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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.traccar.Context;
import org.traccar.helper.Log;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

public class SmppClient {

    private SmppSessionConfiguration sessionConfig = new SmppSessionConfiguration();
    private SmppSession smppSession;
    private DefaultSmppSessionHandler sessionHandler = new ClientSmppSessionHandler(this);
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private DefaultSmppClient clientBootstrap = new DefaultSmppClient(executorService, 1);

    private ScheduledExecutorService enquireLinkExecutor;
    private ScheduledFuture<?> enquireLinkTask;
    private Integer enquireLinkPeriod;
    private Integer enquireLinkTimeout;

    private ScheduledExecutorService reconnectionExecutor;
    private ScheduledFuture<?> reconnectionTask;
    private Integer reconnectionDelay;

    private String sourceAddress;
    private int submitTimeout;
    private String charsetName;
    private byte dataCoding;

    private byte sourceTon;
    private byte sourceNpi;

    private byte destTon;
    private byte destNpi;

    public SmppClient() {
        sessionConfig.setName("Traccar.smppSession");
        sessionConfig.setInterfaceVersion(
                (byte) Context.getConfig().getInteger("sms.smpp.version", SmppConstants.VERSION_3_4));
        sessionConfig.setType(SmppBindType.TRANSCEIVER);
        sessionConfig.setHost(Context.getConfig().getString("sms.smpp.host", "localhost"));
        sessionConfig.setPort(Context.getConfig().getInteger("sms.smpp.port", 2775));
        sessionConfig.setSystemId(Context.getConfig().getString("sms.smpp.username", "user"));
        sessionConfig.setPassword(Context.getConfig().getString("sms.smpp.password", "password"));
        sessionConfig.getLoggingOptions().setLogBytes(false);
        sessionConfig.getLoggingOptions().setLogPdu(Context.getConfig().getBoolean("sms.smpp.logPdu"));

        sourceAddress = Context.getConfig().getString("sms.smpp.sourceAddress", "");
        submitTimeout = Context.getConfig().getInteger("sms.smpp.submitTimeout", 10000);

        charsetName = Context.getConfig().getString("sms.smpp.charsetName", CharsetUtil.NAME_UCS_2);
        dataCoding = (byte) Context.getConfig().getInteger("sms.smpp.dataCoding", SmppConstants.DATA_CODING_UCS2);

        sourceTon = (byte) Context.getConfig().getInteger("sms.smpp.sourceTon", SmppConstants.TON_ALPHANUMERIC);
        sourceNpi = (byte) Context.getConfig().getInteger("sms.smpp.sourceNpi", SmppConstants.NPI_UNKNOWN);

        destTon = (byte) Context.getConfig().getInteger("sms.smpp.destTon", SmppConstants.TON_INTERNATIONAL);
        destNpi = (byte) Context.getConfig().getInteger("sms.smpp.destNpi", SmppConstants.NPI_E164);

        enquireLinkPeriod = Context.getConfig().getInteger("sms.smpp.enquireLinkPeriod", 60000);
        enquireLinkTimeout = Context.getConfig().getInteger("sms.smpp.enquireLinkTimeout", 10000);
        enquireLinkExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                String name = sessionConfig.getName();
                thread.setName("EnquireLink-" + name);
                return thread;
            }
        });

        reconnectionDelay = Context.getConfig().getInteger("sms.smpp.reconnectionDelay", 10000);
        reconnectionExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                String name = sessionConfig.getName();
                thread.setName("Reconnection-" + name);
                return thread;
            }
        });

        scheduleReconnect();
    }

    public synchronized SmppSession getSession() {
        return smppSession;
    }

    public String mapDataCodingToCharset(byte dataCoding) {
        switch (dataCoding) {
            case SmppConstants.DATA_CODING_LATIN1:
                return CharsetUtil.NAME_ISO_8859_1;
            case SmppConstants.DATA_CODING_UCS2:
                return CharsetUtil.NAME_UCS_2;
            default:
                return CharsetUtil.NAME_GSM;
        }
    }

    protected synchronized void reconnect() {
        try {
            disconnect();
            smppSession = clientBootstrap.bind(sessionConfig, sessionHandler);
            stopReconnectionkTask();
            runEnquireLinkTask();
            Log.info("Smpp session connected");
        } catch (SmppTimeoutException | SmppChannelException
                | UnrecoverablePduException | InterruptedException error) {
            Log.warning("Unable to connect to smpp server: ", error);
        }
    }

    public void scheduleReconnect() {
        reconnectionTask = reconnectionExecutor.scheduleWithFixedDelay(
                new ReconnectionTask(this),
                reconnectionDelay, reconnectionDelay, TimeUnit.MILLISECONDS);
    }

    private void stopReconnectionkTask() {
        if (reconnectionTask != null) {
            reconnectionTask.cancel(false);
        }
    }

    private void disconnect() {
        stopEnquireLinkTask();
        destroySession();
    }

    private void runEnquireLinkTask() {
        enquireLinkTask = enquireLinkExecutor.scheduleWithFixedDelay(
                new EnquireLinkTask(this, enquireLinkTimeout),
                enquireLinkPeriod, enquireLinkPeriod, TimeUnit.MILLISECONDS);
    }

    private void stopEnquireLinkTask() {
        if (enquireLinkTask != null) {
            enquireLinkTask.cancel(true);
        }
    }

    private void destroySession() {
        if (smppSession != null) {
            Log.debug("Cleaning up smpp session... ");
            smppSession.destroy();
            smppSession = null;
        }
    }

    public synchronized void sendMessageSync(String destAddress, String message) throws RecoverablePduException,
            UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException {
        if (getSession() != null && getSession().isBound()) {
            byte[] textBytes = CharsetUtil.encode(message, charsetName);

            SubmitSm submit = new SubmitSm();
            submit.setSourceAddress(new Address(sourceTon, sourceNpi, sourceAddress));
            submit.setDestAddress(new Address(destTon, destNpi, destAddress));
            submit.setDataCoding(dataCoding);
            submit.setShortMessage(textBytes);
            SubmitSmResp submitResponce = getSession().submit(submit, submitTimeout);
            Log.debug("SMS submited, msg_id: " + submitResponce.getMessageId());
        } else {
            throw new SmppChannelException("Smpp session is not connected");
        }
    }

    public void sendMessageAsync(final String destAddress, final String message) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    sendMessageSync(destAddress, message);
                } catch (InterruptedException | RecoverablePduException | UnrecoverablePduException
                        | SmppTimeoutException | SmppChannelException error) {
                    Log.warning(error);
                }
            }
        });
    }
}
