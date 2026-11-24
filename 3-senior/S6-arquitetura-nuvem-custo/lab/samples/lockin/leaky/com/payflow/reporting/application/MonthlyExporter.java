package com.payflow.reporting.application;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** "Só um atalho": a aplicação chama o S3 direto (sem porta). */
public class MonthlyExporter {
    private final S3Client s3;

    public MonthlyExporter(S3Client s3) { this.s3 = s3; }

    public void export(String month, byte[] csv) {
        s3.putObject(PutObjectRequest.builder().bucket("payflow-archive").key("statements/" + month + ".csv").build(), RequestBody.fromBytes(csv));
    }
}
