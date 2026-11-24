package com.payflow.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

class FanOutTest {

    static SnsClient sns;
    static SqsClient sqs;

    @BeforeAll
    static void setUp() {
        MotoSupport.requireLocalEmulator();
        sns = AwsClients.sns();
        sqs = AwsClients.sqs();
    }

    private String queue(String name) {
        return sqs.createQueue(CreateQueueRequest.builder().queueName(name).build()).queueUrl();
    }

    private String arn(String url) {
        return sqs.getQueueAttributes(GetQueueAttributesRequest.builder().queueUrl(url)
                .attributeNames(QueueAttributeName.QUEUE_ARN).build()).attributes().get(QueueAttributeName.QUEUE_ARN);
    }

    private void subscribe(String topicArn, String queueArn, String filterEventTypesJson) {
        sns.subscribe(SubscribeRequest.builder().topicArn(topicArn).protocol("sqs").endpoint(queueArn)
                .attributes(Map.of(
                        "RawMessageDelivery", "true",
                        "FilterPolicyScope", "MessageAttributes",
                        "FilterPolicy", "{\"eventType\": " + filterEventTypesJson + "}"))
                .build());
    }

    private List<Message> drain(String url) {
        return sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(url).maxNumberOfMessages(10)
                .waitTimeSeconds(1).build()).messages();
    }

    @Test
    void oneEventReachesEveryInterestedConsumerAndOnlyThem() {
        String topic = sns.createTopic(b -> b.name(MotoSupport.unique("fanout"))).topicArn();
        String notifications = queue(MotoSupport.unique("notifications"));
        String antifraud = queue(MotoSupport.unique("antifraud"));
        subscribe(topic, arn(notifications), "[\"TransferCompleted\", \"TransferFailed\"]");
        subscribe(topic, arn(antifraud), "[\"TransferCompleted\"]");

        EventPublisher publisher = new SnsEventPublisher(sns, topic);
        publisher.publish("TransferCompleted", "acc-1", "{\"amount\":\"10.00\"}");
        publisher.publish("TransferFailed", "acc-1", "{\"amount\":\"99.00\"}");
        publisher.publish("TransferCompleted", "acc-2", "{\"amount\":\"5.00\"}");

        List<Message> forNotifications = drain(notifications);
        List<Message> forAntifraud = drain(antifraud);
        System.out.println("T9.1 notifications received " + forNotifications.size() + " (TransferCompleted + TransferFailed)");
        System.out.println("T9.1 antifraud received     " + forAntifraud.size() + " (TransferCompleted only)");
        System.out.println("T9.1 raw body (no SNS envelope): " + forAntifraud.get(0).body());

        assertThat(forNotifications).hasSize(3);
        assertThat(forAntifraud).hasSize(2);
        assertThat(forAntifraud).extracting(Message::body).doesNotContain("{\"amount\":\"99.00\"}");
        assertThat(forAntifraud.get(0).body()).startsWith("{\"amount\"");
    }
}
