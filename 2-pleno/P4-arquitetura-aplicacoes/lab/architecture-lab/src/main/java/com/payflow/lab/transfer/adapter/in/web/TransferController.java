package com.payflow.lab.transfer.adapter.in.web;

import com.payflow.lab.shared.Money;
import com.payflow.lab.transfer.application.port.in.TransferUseCase;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
class TransferController {

    record TransferRequest(UUID source, UUID target, String amount) { }

    private final TransferUseCase useCase;

    TransferController(TransferUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    Map<String, String> create(@RequestBody TransferRequest request) {
        var id = useCase.transfer(request.source(), request.target(), Money.of(request.amount()));
        return Map.of("id", id.toString());
    }
}
