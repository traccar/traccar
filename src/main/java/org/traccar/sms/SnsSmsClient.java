package org.traccar.sms;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.Context;
import org.traccar.config.Keys;
import org.traccar.notification.MessageException;

import java.util.HashMap;
import java.util.Map;


public class SnsSmsClient implements SmsManager{
    private static final Logger LOGGER = LoggerFactory.getLogger(SnsSmsClient.class);

    private final String access_key;
    private final String secret_key;
    private final String region;

    private final String sns_status;
    private final AmazonSNS snsClient;

    public SnsSmsClient() {
        access_key = Context.getConfig().getString(Keys.AWS_ACCESS_KEY);
        secret_key = Context.getConfig().getString(Keys.AWS_SECRET_KEY);
        sns_status = Context.getConfig().getString(Keys.AWS_SNS_ENABLED);
        region = Context.getConfig().getString(Keys.AWS_REGION);
        snsClient = awsSNSClient(access_key, secret_key, region);

        if (!sns_status.equals("true") || access_key == null || secret_key == null || region == null) {
            LOGGER.error("SNS Not Configured Properly. Please provide valid config.");
        }
    }

    public AmazonSNS awsSNSClient(String access_key, String secret_key, String region) {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(access_key, secret_key);
        return AmazonSNSClientBuilder.standard().withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
    }

    public void sendSNSMessage(String message, String destAddress) {
        Map<String, MessageAttributeValue> smsAttributes = new HashMap<>();
        smsAttributes.put("AWS.SNS.SMS.SenderID",
                new MessageAttributeValue().withStringValue("VegitOne").withDataType("String"));
        smsAttributes.put("AWS.SNS.SMS.SMSType",
                new MessageAttributeValue().withStringValue("Transactional").withDataType("String"));

        PublishResult result = this.snsClient.publish(new PublishRequest().withMessage(message)
                .withPhoneNumber(destAddress).withMessageAttributes(smsAttributes));
    }

    @java.lang.Override
    public void sendMessageSync(String destAddress, String message, boolean command) throws InterruptedException, MessageException {
        sendSNSMessage(message, destAddress);
    }

    @java.lang.Override
    public void sendMessageAsync(String destAddress, String message, boolean command) {
        sendSNSMessage(message, destAddress);
    }
}
