package com.payflow.lab.contract;

import java.util.List;

/** O que o MONÓLITO (consumidor) usa da resposta do serviço de notificações. Só estes dois campos. */
public record Preferences(String userId, List<String> channels) { }
