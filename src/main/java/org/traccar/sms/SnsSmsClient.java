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


public class SnsSmsClient implements SmsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnsSmsClient.class);

    private final String accessKey;
    private final String secretKey;
    private final String region;

    private final String snsStatus;
    private final AmazonSNS snsClient;

    public SnsSmsClient() {
        accessKey = Context.getConfig().getString(Keys.AWS_ACCESS_KEY);
        secretKey = Context.getConfig().getString(Keys.AWS_SECRET_KEY);
        snsStatus = Context.getConfig().getString(Keys.AWS_SNS_ENABLED);
        region = Context.getConfig().getString(Keys.AWS_REGION);
        snsClient = awsSNSClient(accessKey, secretKey, region);

        if (!snsStatus.equals("true") || accessKey == null || secretKey == null || region == null) {
            LOGGER.error("SNS Not Configured Properly. Please provide valid config.");
        }
    }

    public AmazonSNS awsSNSClient(String accessKey, String secretKey, String region) {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonSNSClientBuilder.standard().withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
    }

    public void sendSNSMessage(String message, String destAddress) {
        Map<String, MessageAttributeValue> smsAttributes = new HashMap<>();
        smsAttributes.put("AWS.SNS.SMS.SenderID",
                new MessageAttributeValue().withStringValue("SNS").withDataType("String"));
        smsAttributes.put("AWS.SNS.SMS.SMSType",
                new MessageAttributeValue().withStringValue("Transactional").withDataType("String"));

        PublishResult result = this.snsClient.publish(new PublishRequest().withMessage(message)
                .withPhoneNumber(destAddress).withMessageAttributes(smsAttributes));
    }

    @java.lang.Override
    public void sendMessageSync(String destAddress, String message, boolean command)
            throws InterruptedException, MessageException {
        sendSNSMessage(message, destAddress);
    }

    @java.lang.Override
    public void sendMessageAsync(String destAddress, String message, boolean command) {
        sendSNSMessage(message, destAddress);
    }
}
