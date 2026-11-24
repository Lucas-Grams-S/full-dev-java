package com.payflow.assistant;

/** What the support assistant may see about ONE transfer. The memo is typed by a user: UNTRUSTED text. */
public record TransferSnapshot(String id, String ownerId, String status, String amount, String memo) {}
