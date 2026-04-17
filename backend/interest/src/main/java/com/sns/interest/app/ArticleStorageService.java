package com.sns.interest.app;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Stores article binaries in S3-compatible storage (MinIO in dev). Returns the object key used to
 * reconstruct a pre-signed URL later. When storage is not configured, falls back to an in-memory
 * map — fine for tests, never for prod.
 */
@Service
public class ArticleStorageService {

    private static final Logger log = LoggerFactory.getLogger(ArticleStorageService.class);

    private final ConcurrentMap<String, byte[]> inMemory = new ConcurrentHashMap<>();

    private final S3Client client;
    private final String bucket;
    private final boolean usingS3;

    public ArticleStorageService(
        @Value("${sns.storage.endpoint:}") String endpoint,
        @Value("${sns.storage.region:us-east-1}") String region,
        @Value("${sns.storage.bucket:sns-articles}") String bucket,
        @Value("${sns.storage.access-key:}") String accessKey,
        @Value("${sns.storage.secret-key:}") String secretKey
    ) {
        this.bucket = bucket;
        if (endpoint == null || endpoint.isBlank() || accessKey == null || accessKey.isBlank()) {
            log.warn("Object storage disabled — articles will be kept in process memory");
            this.client = null;
            this.usingS3 = false;
        } else {
            this.client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
            this.usingS3 = true;
        }
    }

    public String store(UUID userId, String filename, byte[] bytes, String contentType) {
        String key = "articles/" + userId + "/" + UUID.randomUUID() + "-" + safeName(filename);
        if (usingS3) {
            client.putObject(PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build(),
                RequestBody.fromInputStream(new ByteArrayInputStream(bytes), bytes.length));
        } else {
            inMemory.put(key, bytes);
        }
        return key;
    }

    public byte[] read(String key) {
        if (usingS3) {
            return client.getObjectAsBytes(b -> b.bucket(bucket).key(key)).asByteArray();
        }
        byte[] data = inMemory.get(key);
        if (data == null) throw new IllegalArgumentException("No such object: " + key);
        return data;
    }

    private static String safeName(String filename) {
        if (filename == null) return "article";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
