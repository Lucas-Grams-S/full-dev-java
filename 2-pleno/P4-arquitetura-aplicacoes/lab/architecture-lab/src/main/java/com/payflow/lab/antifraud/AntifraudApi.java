package com.payflow.lab.antifraud;

import java.util.List;
import java.util.UUID;

public interface AntifraudApi {
    List<UUID> flaggedTransfers();
}
