package com.payflow.cloud;

import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Production-shaped clients: NO endpoint, NO credentials, NO region here.
 * The default chains find them (ECS task role, AWS_REGION, and AWS_ENDPOINT_URL when pointing to an emulator).
 * One client per service, created ONCE and shared (they are thread-safe and expensive to build).
 */
public final class AwsClients {

    private AwsClients() {
    }

    public static SnsClient sns() {
        return SnsClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder()).build();
    }

    public static SqsClient sqs() {
        return SqsClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder()).build();
    }

    public static SecretsManagerClient secrets() {
        return SecretsManagerClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder()).build();
    }
}
