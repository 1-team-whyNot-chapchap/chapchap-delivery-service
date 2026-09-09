package com.chapchap.delivery.global.kafka.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KafkaConfigurationContractTest {
    @Test
    @DisplayName("환경변수 예시는 msa4-team1 Kafka Topic과 확정 Consumer 설정을 사용한다")
    void environmentExampleUsesCurrentKafkaContract() throws IOException {
        String environment = Files.readString(Path.of(".env.example"));

        assertThat(environment)
            .contains("KAFKA_TOPIC_AUTH_USER=msa4-team1.auth.user-events.v1")
            .contains("KAFKA_TOPIC_SUBSCRIPTION_DELIVERY_ORDER=msa4-team1.subscription.delivery-orders.v1")
            .contains("KAFKA_TOPIC_DELIVERY_EVENT=msa4-team1.delivery.delivery-events.v1")
            .contains("KAFKA_TOPIC_DELIVERY_REFUND_EVENT=msa4-team1.delivery.refund-events.v1")
            .contains("KAFKA_TOPIC_OPERATION_NOTIFICATION=msa4-team1.delivery.operation-notification-requests.v1")
            .contains("DELIVERY_AUTH_CONSUMER_GROUP=delivery-service-auth-group")
            .contains("DELIVERY_ORDER_CONSUMER_GROUP=delivery-service-order-group")
            .contains("KAFKA_RETRY_INTERVAL_MS=1000")
            .contains("KAFKA_MAX_RETRIES=3");
    }

    @Test
    @DisplayName("application.yaml은 확정 환경변수 Key에서 Kafka 설정을 주입한다")
    void applicationUsesCurrentKafkaEnvironmentKeys() throws IOException {
        String application = Files.readString(Path.of("src/main/resources/application.yaml"));

        assertThat(application)
            .contains("${KAFKA_TOPIC_AUTH_USER}")
            .contains("${KAFKA_TOPIC_SUBSCRIPTION_DELIVERY_ORDER}")
            .contains("${KAFKA_TOPIC_DELIVERY_EVENT}")
            .contains("${KAFKA_TOPIC_DELIVERY_REFUND_EVENT}")
            .contains("${KAFKA_TOPIC_OPERATION_NOTIFICATION}")
            .contains("${DELIVERY_AUTH_CONSUMER_GROUP}")
            .contains("${DELIVERY_ORDER_CONSUMER_GROUP}")
            .contains("${KAFKA_RETRY_INTERVAL_MS}")
            .contains("${KAFKA_MAX_RETRIES}");
    }

    @Test
    @DisplayName("환경변수 예시는 확정된 완료 사진 업무 제한을 사용한다")
    void environmentExampleUsesCurrentPhotoContract() throws IOException {
        String environment = Files.readString(Path.of(".env.example"));

        assertThat(environment)
            .contains("MINIO_BUCKET=msa4-team1")
            .contains("MINIO_IMAGE_PATH=delivery/completion-photos")
            .contains("DELIVERY_PHOTO_MAX_FILE_SIZE=10MB")
            .contains("DELIVERY_PHOTO_MAX_REQUEST_SIZE=11MB")
            .contains("DELIVERY_PHOTO_MAX_FILE_SIZE_BYTES=10485760")
            .contains("DELIVERY_PHOTO_PRESIGNED_GET_EXPIRATION=10m")
            .doesNotContain("MINIO_DELIVERY_PHOTO_BUCKET")
            .doesNotContain("DELIVERY_PHOTO_ALLOWED_CONTENT_TYPES");

        String application = Files.readString(Path.of("src/main/resources/application.yaml"));
        assertThat(application)
            .contains("minio-bucket: ${MINIO_BUCKET}")
            .contains("minio-image-path: ${MINIO_IMAGE_PATH}")
            .contains("allow-image-extensions:")
            .contains("\"image/jpg\"")
            .contains("\"image/jpeg\"")
            .contains("\"image/png\"")
            .contains("\"image/gif\"")
            .contains("\"image/webp\"");
    }
}
