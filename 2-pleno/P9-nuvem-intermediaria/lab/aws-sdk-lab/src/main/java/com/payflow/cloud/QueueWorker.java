package com.payflow.cloud;

import java.util.List;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

/**
 * One polling round: receive (long polling), handle, DELETE only after success.
 * If the handler throws, the message is NOT deleted: it reappears after the visibility timeout and, after
 * maxReceiveCount deliveries, SQS moves it to the dead-letter queue by itself.
 */
public final class QueueWorker {

    private final SqsClient sqs;
    private final String queueUrl;
    private final IdempotentHandler handler;
    private final int waitSeconds;

    public QueueWorker(SqsClient sqs, String queueUrl, IdempotentHandler handler, int waitSeconds) {
        this.sqs = sqs;
        this.queueUrl = queueUrl;
        this.handler = handler;
        this.waitSeconds = waitSeconds;
    }

    /** @return how many messages were received in this round (including failures and duplicates) */
    public int pollOnce() {
        List<Message> messages = sqs.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(waitSeconds)
                .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
                .build()).messages();
        for (Message m : messages) {
            try {
                handler.handle(m.messageId(), m.body());
                sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(m.receiptHandle()).build());
            } catch (RuntimeException e) {
                // keep the message: no delete. Log with the receive count so the operator sees the retry climbing.
                System.out.printf("handler failed (receiveCount=%s): %s%n",
                        m.attributes().get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT), e.getMessage());
            }
        }
        return messages.size();
    }
}
