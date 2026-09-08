package com.chapchap.delivery.global.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MinioDeliveryPhotoStorageTest {
    @Test
    @DisplayName("Delivery 사진은 팀 공용 MinIO 버킷을 사용한다")
    void usesFixedTeamBucket() {
        assertThat(MinioDeliveryPhotoStorage.BUCKET_NAME).isEqualTo("msa4-team1");
    }

    @Test
    @DisplayName("MinIO 버킷 이름을 환경 설정으로 받지 않는다")
    void doesNotConfigureBucketFromEnvironment() throws IOException {
        String application = Files.readString(Path.of("src/main/resources/application.yaml"));
        String environment = Files.readString(Path.of(".env.example"));

        assertThat(application).doesNotContain("MINIO_DELIVERY_PHOTO_BUCKET", "bucket:");
        assertThat(environment).doesNotContain("MINIO_DELIVERY_PHOTO_BUCKET");
    }
}
