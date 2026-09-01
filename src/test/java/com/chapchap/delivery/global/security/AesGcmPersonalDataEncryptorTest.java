package com.chapchap.delivery.global.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmPersonalDataEncryptorTest {

    private static final String VALID_KEY =
        Base64.getEncoder().encodeToString(
            "12345678901234567890123456789012"
                .getBytes(StandardCharsets.UTF_8)
        );

    @Test
    void 평문을_암호화하면_평문과_다른_값이_생성된다() {
        AesGcmPersonalDataEncryptor encryptor =
            new AesGcmPersonalDataEncryptor(VALID_KEY);

        String plainText = "010-1234-5678";

        byte[] encrypted = encryptor.encrypt(plainText);

        assertThat(encrypted)
            .isNotEmpty();

        assertThat(encrypted)
            .isNotEqualTo(
                plainText.getBytes(StandardCharsets.UTF_8)
            );
    }

    @Test
    void 같은_평문을_암호화해도_매번_다른_암호문이_생성된다() {
        AesGcmPersonalDataEncryptor encryptor =
            new AesGcmPersonalDataEncryptor(VALID_KEY);

        String plainText = "010-1234-5678";

        byte[] first = encryptor.encrypt(plainText);
        byte[] second = encryptor.encrypt(plainText);

        assertThat(first)
            .isNotEqualTo(second);
    }

    @Test
    void Base64가_아닌_키를_사용하면_객체_생성에_실패한다() {
        assertThatThrownBy(
            () -> new AesGcmPersonalDataEncryptor(
                "not-base64-key!!"
            )
        )
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void AES_256이_아닌_키를_사용하면_객체_생성에_실패한다() {
        String shortKey =
            Base64.getEncoder().encodeToString(
                "too-short"
                    .getBytes(StandardCharsets.UTF_8)
            );

        assertThatThrownBy(
            () -> new AesGcmPersonalDataEncryptor(shortKey)
        )
            .isInstanceOf(IllegalStateException.class);
    }
}