package com.payflow.reporting.adapter.out.s3;

import com.payflow.reporting.application.MonthlyExporter;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3ArchiveStore implements MonthlyExporter.ArchiveStore {
    private final S3Client s3;
    private final String bucket;

    public S3ArchiveStore(S3Client s3, String bucket) { this.s3 = s3; this.bucket = bucket; }

    @Override
    public void put(String key, byte[] content) {
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(), RequestBody.fromBytes(content));
    }
}
