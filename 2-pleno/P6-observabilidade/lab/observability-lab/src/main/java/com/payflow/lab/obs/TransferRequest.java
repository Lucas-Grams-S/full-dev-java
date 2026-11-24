package com.payflow.lab.obs;

public record TransferRequest(String from, String to, String amount, String simulate) {
}
