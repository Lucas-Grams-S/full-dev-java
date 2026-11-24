package com.payflow.reporting.application;

/** Porta de saída para o arquivo do extrato mensal: S3 hoje, outro armazenamento amanhã. */
public class MonthlyExporter {
    public interface ArchiveStore { void put(String key, byte[] content); }

    private final ArchiveStore store;

    public MonthlyExporter(ArchiveStore store) { this.store = store; }

    public void export(String month, byte[] csv) { store.put("statements/" + month + ".csv", csv); }
}
