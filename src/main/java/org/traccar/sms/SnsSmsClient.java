/*
 * Copyright 2021 - 2025 Anton Tananaev (anton@traccar.org)
 * Copyright 2021 Subodh Ranadive (subodhranadive3103@gmail.com)
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
package org.traccar.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.HashMap;
import java.util.Map;

public class SnsSmsClient implements SmsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnsSmsClient.class);

    private final SnsClient snsClient;

    public SnsSmsClient(Config config) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                config.getString(Keys.SMS_AWS_ACCESS), config.getString(Keys.SMS_AWS_SECRET));
        snsClient = SnsClient.builder()
                .region(Region.of(config.getString(Keys.SMS_AWS_REGION)))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }

    @Override
    public void sendMessage(String phone, String message, boolean command) {
        Map<String, MessageAttributeValue> smsAttributes = new HashMap<>();
        smsAttributes.put(
                "AWS.SNS.SMS.SenderID",
                MessageAttributeValue.builder()
                        .stringValue("SNS")
                        .dataType("String")
                        .build());
        smsAttributes.put(
                "AWS.SNS.SMS.SMSType",
                MessageAttributeValue.builder()
                        .stringValue("Transactional")
                        .dataType("String")
                        .build());

        PublishRequest publishRequest = PublishRequest.builder()
                .message(message)
                .phoneNumber(phone)
                .messageAttributes(smsAttributes)
                .build();

        try {
            snsClient.publish(publishRequest);
        } catch (SnsException e) {
            LOGGER.error("SMS send failed", e);
        }
    }
}
