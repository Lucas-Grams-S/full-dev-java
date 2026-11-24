package com.payflow.cloud;

/** Output port of the application (P4): "publish a domain event". The AWS SDK never leaks past the adapter. */
public interface EventPublisher {

    /**
     * @param eventType used by subscribers to FILTER (SNS message attribute {@code eventType})
     * @param key       ordering key (FIFO MessageGroupId): events of the same account stay in order
     * @param json      the event, money as a string (decision 11)
     */
    void publish(String eventType, String key, String json);
}
