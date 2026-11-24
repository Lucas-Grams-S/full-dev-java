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

class VisibilityTimeoutTest {

    static SqsClient sqs;

    @BeforeAll
    static void setUp() {
        MotoSupport.requireLocalEmulator();
        sqs = AwsClients.sqs();
    }

    private Message receive(String url) {
        List<Message> m = sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(url).waitTimeSeconds(1)
                .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT).build()).messages();
        return m.isEmpty() ? null : m.get(0);
    }

    @Test
    void aConsumerThatDiesWithoutDeletingCausesADuplicateAndTheIdempotentHandlerAbsorbsIt() throws Exception {
        String url = sqs.createQueue(CreateQueueRequest.builder().queueName(MotoSupport.unique("visibility"))
                .attributes(Map.of(QueueAttributeName.VISIBILITY_TIMEOUT, "1")).build()).queueUrl();
        sqs.sendMessage(b -> b.queueUrl(url).messageBody("transfer-42"));

        Message first = receive(url);                    // consumer A takes it ... and dies (never deletes)
        Message hidden = receive(url);                   // while invisible, nobody else sees it
        Thread.sleep(1500);                              // visibility timeout (1 s) expires
        Message second = receive(url);                   // consumer B gets the SAME message again

        System.out.println("T9.3 first receive : id=" + first.messageId().substring(0, 8) + " receiveCount="
                + first.attributes().get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT));
        System.out.println("T9.3 while hidden  : " + (hidden == null ? "nothing (message is invisible)" : "visible?!"));
        System.out.println("T9.3 second receive: id=" + second.messageId().substring(0, 8) + " receiveCount="
                + second.attributes().get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT));

        List<String> effects = new ArrayList<>();
        IdempotentHandler handler = new IdempotentHandler(effects::add);
        boolean a = handler.handle(first.messageId(), first.body());
        boolean b = handler.handle(second.messageId(), second.body());
        System.out.println("T9.3 handler ran for delivery 1: " + a + ", for delivery 2: " + b + " -> effects=" + effects);

        // the CONTRAST that proves the handler matters: without the id check the effect happens twice
        List<String> naive = new ArrayList<>();
        naive.add(first.body());
        naive.add(second.body());
        System.out.println("T9.3 a naive consumer (no id check) would have produced: " + naive);

        assertThat(naive).containsExactly("transfer-42", "transfer-42");
        assertThat(hidden).isNull();
        assertThat(second.messageId()).isEqualTo(first.messageId());
        assertThat(second.attributes().get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)).isEqualTo("2");
        assertThat(effects).containsExactly("transfer-42");
    }
}
