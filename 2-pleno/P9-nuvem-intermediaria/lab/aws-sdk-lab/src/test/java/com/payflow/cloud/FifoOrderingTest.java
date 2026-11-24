package com.payflow.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

class FifoOrderingTest {

    static SqsClient sqs;

    @BeforeAll
    static void setUp() {
        MotoSupport.requireLocalEmulator();
        sqs = AwsClients.sqs();
    }

    @Test
    void fifoKeepsTheOrderInsideAGroupAndDropsTheDuplicateWithTheSameDeduplicationId() {
        String url = sqs.createQueue(CreateQueueRequest.builder().queueName(MotoSupport.unique("ledger") + ".fifo")
                .attributes(Map.of(QueueAttributeName.FIFO_QUEUE, "true",
                        QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "false")).build()).queueUrl();

        // two accounts interleaved; the SAME deduplication id sent twice for deposit-2 of acc-1 (a client retry)
        send(url, "acc-1", "d1", "acc-1:deposit-1");
        send(url, "acc-2", "d2", "acc-2:deposit-1");
        send(url, "acc-1", "d3", "acc-1:deposit-2");
        send(url, "acc-1", "d3", "acc-1:deposit-2"); // duplicate: same MessageDeduplicationId inside the deduplication window
        send(url, "acc-1", "d4", "acc-1:deposit-3");
        send(url, "acc-2", "d5", "acc-2:deposit-2");

        List<String> received = new ArrayList<>();
        for (int round = 0; round < 5; round++) {
            for (Message m : sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(url).maxNumberOfMessages(10)
                    .waitTimeSeconds(1).messageSystemAttributeNames(MessageSystemAttributeName.MESSAGE_GROUP_ID).build()).messages()) {
                received.add(m.body());
                sqs.deleteMessage(b -> b.queueUrl(url).receiptHandle(m.receiptHandle()));
            }
        }
        List<String> acc1 = received.stream().filter(s -> s.startsWith("acc-1")).toList();
        List<String> acc2 = received.stream().filter(s -> s.startsWith("acc-2")).toList();
        System.out.println("T9.4 sent 6 messages (1 duplicate dedup id), received " + received.size());
        System.out.println("T9.4 acc-1 in order: " + acc1);
        System.out.println("T9.4 acc-2 in order: " + acc2);

        assertThat(received).hasSize(5);
        assertThat(acc1).containsExactly("acc-1:deposit-1", "acc-1:deposit-2", "acc-1:deposit-3");
        assertThat(acc2).containsExactly("acc-2:deposit-1", "acc-2:deposit-2");
    }

    private void send(String url, String group, String dedup, String body) {
        sqs.sendMessage(b -> b.queueUrl(url).messageGroupId(group).messageDeduplicationId(dedup).messageBody(body));
    }
}
