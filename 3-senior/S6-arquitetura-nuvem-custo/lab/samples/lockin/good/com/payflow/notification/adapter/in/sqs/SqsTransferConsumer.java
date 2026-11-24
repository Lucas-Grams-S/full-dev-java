package com.payflow.notification.adapter.in.sqs;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/** Adaptador de entrada (P9.04): recebe, processa de forma idempotente e só então apaga. */
public class SqsTransferConsumer {
    private final SqsClient sqs;
    private final String queueUrl;

    public SqsTransferConsumer(SqsClient sqs, String queueUrl) { this.sqs = sqs; this.queueUrl = queueUrl; }

    public void pollOnce(java.util.function.Consumer<String> handler) {
        var msgs = sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).waitTimeSeconds(20).build()).messages();
        for (var m : msgs) {
            handler.accept(m.body());
            sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(m.receiptHandle()).build());
        }
    }
}
