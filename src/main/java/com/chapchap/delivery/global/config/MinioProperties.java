package com.chapchap.delivery.global.config;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minio")
public record MinioProperties(
    String minioEndpoint
    , String minioBucket
    , String minioAccessKey
    , String minioSecretKey
    , String minioImagePath
    , Set<String> allowImageExtensions
) {
    public String resolveImageObjectKey(String filename) {
        String path = minioImagePath == null ? "" : minioImagePath.trim();
        path = path.replaceAll("^/+|/+$", "");
        return path.isEmpty() ? filename : path + "/" + filename;
    }
}
