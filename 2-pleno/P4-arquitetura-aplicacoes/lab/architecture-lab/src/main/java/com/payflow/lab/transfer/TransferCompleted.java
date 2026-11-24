package com.payflow.lab.transfer;

import com.payflow.lab.shared.Money;
import java.util.UUID;

/** Evento publicado pelo módulo transfer (parte da API pública dele). Imutável, no passado, sem lógica. */
public record TransferCompleted(UUID transferId, UUID sourceAccountId, UUID targetAccountId, Money amount) {
}
