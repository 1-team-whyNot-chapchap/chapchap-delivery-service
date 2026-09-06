package com.chapchap.delivery.global.storage;

import com.chapchap.delivery.global.config.DeliveryPhotoStorageProperties;
import com.chapchap.delivery.global.exception.TechnicalException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Http;
import java.io.InputStream;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class MinioDeliveryPhotoStorage implements DeliveryPhotoStorage {
    private final DeliveryPhotoStorageProperties properties;
    private final MinioClient client;

    public MinioDeliveryPhotoStorage(DeliveryPhotoStorageProperties properties) {
        this.properties = properties;
        this.client = MinioClient.builder()
            .endpoint(properties.endpoint())
            .credentials(properties.accessKey(), properties.secretKey())
            .build();
    }

    @Override
    public void upload(
        String objectKey
        , InputStream inputStream
        , long fileSize
        , String contentType
    ) {
        try {
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(inputStream, fileSize, null)
                    .contentType(contentType)
                    .build()
            );
        } catch (Exception exception) {
            throw new TechnicalException("Failed to store delivery completion photo", exception);
        }
    }

    @Override
    public String createPresignedGetUrl(String objectKey, Duration validity) {
        try {
            return client.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Http.Method.GET)
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .expiry(Math.toIntExact(validity.toSeconds()))
                    .build()
            );
        } catch (Exception exception) {
            throw new TechnicalException("Failed to issue delivery photo access URL", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build()
            );
        } catch (Exception exception) {
            throw new TechnicalException("Failed to delete delivery photo object", exception);
        }
    }
}
