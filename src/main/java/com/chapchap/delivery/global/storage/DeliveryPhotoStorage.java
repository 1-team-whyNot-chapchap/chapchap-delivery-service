package com.chapchap.delivery.global.storage;

import java.io.InputStream;
import java.time.Duration;

public interface DeliveryPhotoStorage {
    void upload(
        String objectKey
        , InputStream inputStream
        , long fileSize
        , String contentType
    );

    String createPresignedGetUrl(String objectKey, Duration validity);

    void delete(String objectKey);
}
