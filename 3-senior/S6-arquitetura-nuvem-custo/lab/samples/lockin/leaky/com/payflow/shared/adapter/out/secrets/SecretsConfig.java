package com.payflow.shared.adapter.out.secrets;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

/** Só para tarefas fora do ECS; no ECS o segredo chega por valueFrom (decisão 169). */
public class SecretsConfig {
    public static String read(SecretsManagerClient sm, String id) {
        return sm.getSecretValue(GetSecretValueRequest.builder().secretId(id).build()).secretString();
    }
}
