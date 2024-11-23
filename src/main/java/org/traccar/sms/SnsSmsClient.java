/*
 * Copyright 2021 - 2022 Anton Tananaev (anton@traccar.org)
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

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;

import com.amazonaws.services.sns.model.PublishResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;

import java.util.HashMap;
import java.util.Map;

public class SnsSmsClient implements SmsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnsSmsClient.class);

    private final AmazonSNSAsync snsClient;

    public SnsSmsClient(Config config) {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(
                config.getString(Keys.SMS_AWS_ACCESS), config.getString(Keys.SMS_AWS_SECRET));
        snsClient = AmazonSNSAsyncClientBuilder.standard()
                .withRegion(config.getString(Keys.SMS_AWS_REGION))
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials)).build();
    }

    @Override
    public void sendMessage(String phone, String message, boolean command) {
        Map<String, MessageAttributeValue> smsAttributes = new HashMap<>();
        smsAttributes.put(
                "AWS.SNS.SMS.SenderID",
                new MessageAttributeValue().withStringValue("SNS").withDataType("String"));
        smsAttributes.put(
                "AWS.SNS.SMS.SMSType",
                new MessageAttributeValue().withStringValue("Transactional").withDataType("String"));

        PublishRequest publishRequest = new PublishRequest()
                .withMessage(message)
                .withPhoneNumber(phone)
                .withMessageAttributes(smsAttributes);

        snsClient.publishAsync(publishRequest, new AsyncHandler<>() {
            @Override
            public void onError(Exception exception) {
                LOGGER.error("SMS send failed", exception);
            }

            @Override
            public void onSuccess(PublishRequest request, PublishResult result) {
            }
        });
    }
}
