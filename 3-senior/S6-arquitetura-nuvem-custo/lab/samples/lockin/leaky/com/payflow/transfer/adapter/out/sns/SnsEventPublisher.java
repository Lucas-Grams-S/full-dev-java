package com.payflow.transfer.adapter.out.sns;

import com.payflow.transfer.application.port.out.EventPublisher;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

/** Adaptador (P9.04): o único lugar que conhece o SNS. */
public class SnsEventPublisher implements EventPublisher {
    private final SnsClient sns;
    private final String topicArn;

    public SnsEventPublisher(SnsClient sns, String topicArn) { this.sns = sns; this.topicArn = topicArn; }

    @Override
    public void publish(String eventType, String key, String jsonPayload) {
        sns.publish(PublishRequest.builder().topicArn(topicArn).message(jsonPayload)
                .messageAttributes(Map.of("eventType", MessageAttributeValue.builder().dataType("String").stringValue(eventType).build()))
                .build());
    }
}
