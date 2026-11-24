package com.payflow.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

class PoisonMessageTest {

    static SqsClient sqs;

    @BeforeAll
    static void setUp() {
        MotoSupport.requireLocalEmulator();
        sqs = AwsClients.sqs();
    }

    @Test
    void aPoisonMessageGoesToTheDeadLetterQueueAndTheOthersAreNotBlocked() throws Exception {
        String dlqUrl = sqs.createQueue(CreateQueueRequest.builder().queueName(MotoSupport.unique("poison-dlq")).build()).queueUrl();
        String dlqArn = sqs.getQueueAttributes(GetQueueAttributesRequest.builder().queueUrl(dlqUrl)
                .attributeNames(QueueAttributeName.QUEUE_ARN).build()).attributes().get(QueueAttributeName.QUEUE_ARN);
        String url = sqs.createQueue(CreateQueueRequest.builder().queueName(MotoSupport.unique("poison-main"))
                .attributes(Map.of(
                        QueueAttributeName.VISIBILITY_TIMEOUT, "1",
                        QueueAttributeName.REDRIVE_POLICY, "{\"deadLetterTargetArn\":\"" + dlqArn + "\",\"maxReceiveCount\":3}"))
                .build()).queueUrl();

        for (String body : List.of("ok-1", "poison", "ok-2")) {
            sqs.sendMessage(b -> b.queueUrl(url).messageBody(body));
        }

        List<String> done = new ArrayList<>();
        int[] poisonAttempts = {0};
        IdempotentHandler handler = new IdempotentHandler(body -> {
            if (body.equals("poison")) {
                poisonAttempts[0]++;
                throw new IllegalStateException("cannot parse event");
            }
            done.add(body);
        });
        QueueWorker worker = new QueueWorker(sqs, url, handler, 1);

        long deadline = System.currentTimeMillis() + 20_000;
        List<Message> inDlq = List.of();
        while (System.currentTimeMillis() < deadline && inDlq.isEmpty()) {
            worker.pollOnce();
            inDlq = sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(dlqUrl).waitTimeSeconds(0)
                    .visibilityTimeout(0).build()).messages();
        }

        System.out.println("T9.2 processed normally: " + done);
        System.out.println("T9.2 poison handler attempts before the DLQ: " + poisonAttempts[0] + " (maxReceiveCount=3)");
        System.out.println("T9.2 dead-letter queue holds: " + inDlq.stream().map(Message::body).toList());

        assertThat(done).containsExactlyInAnyOrder("ok-1", "ok-2");
        assertThat(inDlq).extracting(Message::body).containsExactly("poison");
        assertThat(poisonAttempts[0]).isEqualTo(3);
    }
}
