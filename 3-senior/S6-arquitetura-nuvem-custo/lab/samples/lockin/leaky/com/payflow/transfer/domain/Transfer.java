package com.payflow.transfer.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;
import java.util.UUID;

/** O domínio virou um item do DynamoDB: anotações do provedor dentro da entidade. */
@DynamoDbBean
public class Transfer {
    private UUID id;
    private BigDecimal amount;

    @DynamoDbPartitionKey
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
