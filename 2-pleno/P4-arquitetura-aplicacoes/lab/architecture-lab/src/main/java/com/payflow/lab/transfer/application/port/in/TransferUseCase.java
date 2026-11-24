package com.payflow.lab.transfer.application.port.in;

import com.payflow.lab.shared.Money;
import java.util.UUID;

/** Porta de ENTRADA: o que o mundo de fora pode pedir ao módulo. */
public interface TransferUseCase {
    UUID transfer(UUID source, UUID target, Money amount);
}
