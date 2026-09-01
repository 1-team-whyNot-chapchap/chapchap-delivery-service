package com.chapchap.delivery.global.kafka.config;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public ProducerFactory<String, Object> kafkaProducerFactory(
        KafkaProperties kafkaProperties
    ) {
        Map<Class<?>, Serializer<?>> serializers =
            new LinkedHashMap<>();

        serializers.put(
            byte[].class
            , new ByteArraySerializer()
        );

        serializers.put(
            Object.class
            , new JacksonJsonSerializer<Object>()
                .noTypeInfo()
        );

        DelegatingByTypeSerializer valueSerializer =
            new DelegatingByTypeSerializer(
                serializers
                , true
            );

        return new DefaultKafkaProducerFactory<>(
            kafkaProperties.buildProducerProperties()
            , new StringSerializer()
            , valueSerializer
        );
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
        ProducerFactory<String, Object> kafkaProducerFactory
    ) {
        return new KafkaTemplate<>(
            kafkaProducerFactory
        );
    }

    @Bean
    public DeadLetterPublishingRecoverer
    deadLetterPublishingRecoverer(
        KafkaTemplate<String, Object> kafkaTemplate
    ) {

        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(
                kafkaTemplate
                , (record, exception) ->
                new TopicPartition(
                    record.topic() + ".DLT"
                    , record.partition()
                )
            );

        recoverer.setFailIfSendResultIsError(true);

        return recoverer;
    }

    @Bean
    public CommonErrorHandler kafkaCommonErrorHandler(
        DeadLetterPublishingRecoverer recoverer
        , @Value("${kafka.retry.interval-ms}")
        long retryIntervalMs
        , @Value("${kafka.retry.max-retries}")
        long maxRetries
    ) {
        FixedBackOff fixedBackOff =
            new FixedBackOff(
                retryIntervalMs
                , maxRetries
            );

        DefaultErrorHandler errorHandler =
            new DefaultErrorHandler(
                recoverer
                , fixedBackOff
            );

        errorHandler.removeClassification(
            DeserializationException.class
        );

        return errorHandler;
    }
}