package com.payflow.cloud;

import java.util.Map;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/** Adapter: publishes to an SNS topic. Standard topic: no ordering guarantee (see the FIFO test for the alternative). */
public final class SnsEventPublisher implements EventPublisher {

    private final SnsClient sns;
    private final String topicArn;

    public SnsEventPublisher(SnsClient sns, String topicArn) {
        this.sns = sns;
        this.topicArn = topicArn;
    }

    @Override
    public void publish(String eventType, String key, String json) {
        sns.publish(PublishRequest.builder()
                .topicArn(topicArn)
                .message(json)
                .messageAttributes(Map.of(
                        "eventType", MessageAttributeValue.builder().dataType("String").stringValue(eventType).build(),
                        "aggregateId", MessageAttributeValue.builder().dataType("String").stringValue(key).build()))
                .build());
    }
}
