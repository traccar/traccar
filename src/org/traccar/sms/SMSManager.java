package org.traccar.sms;

public interface SMSManager {

    void sendMessageSync(String destAddress, String message, boolean command) throws InterruptedException, SMSException;
    void sendMessageAsync(final String destAddress, final String message, final boolean command);

}
