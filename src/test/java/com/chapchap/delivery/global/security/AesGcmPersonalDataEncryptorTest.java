package com.chapchap.delivery.global.security;

import com.chapchap.delivery.global.exception.technical.PersonalDataEncryptionException;
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

    private static final String OTHER_VALID_KEY =
        Base64.getEncoder().encodeToString(
            "abcdefghijklmnopqrstuvwxyz123456"
                .getBytes(StandardCharsets.UTF_8)
        );

    @Test
    void 평문을_암호화하면_평문과_다른_값이_생성된다() {
        AesGcmPersonalDataEncryptor encryptor =
            new AesGcmPersonalDataEncryptor(
                VALID_KEY
            );

        String plainText =
            "010-1234-5678";

        byte[] encrypted =
            encryptor.encrypt(
                plainText
            );

        assertThat(encrypted)
            .isNotEmpty();

        assertThat(encrypted)
            .isNotEqualTo(
                plainText.getBytes(
                    StandardCharsets.UTF_8
                )
            );
    }

    @Test
    void 같은_평문을_암호화해도_매번_다른_암호문이_생성된다() {
        AesGcmPersonalDataEncryptor encryptor =
            new AesGcmPersonalDataEncryptor(
                VALID_KEY
            );

        String plainText =
            "010-1234-5678";

        byte[] first =
            encryptor.encrypt(
                plainText
            );

        byte[] second =
            encryptor.encrypt(
                plainText
            );

        assertThat(first)
            .isNotEqualTo(second);
    }

    @Test
    void 암호화한_개인정보를_복호화하면_원본_평문을_얻는다() {
        AesGcmPersonalDataEncryptor encryptor =
            new AesGcmPersonalDataEncryptor(
                VALID_KEY
            );

        String plainText =
            "010-1234-5678";

        byte[] encrypted =
            encryptor.encrypt(
                plainText
            );

        String decrypted =
            encryptor.decrypt(
                encrypted
            );

        assertThat(decrypted)
            .isEqualTo(
                plainText
            );
    }

    @Test
    void 한글_개인정보도_암호화_후_복호화할_수_있다() {
        AesGcmPersonalDataEncryptor encryptor =
            new AesGcmPersonalDataEncryptor(
                VALID_KEY
            );

        String plainText =
            "공동현관 비밀번호 1234";

        byte[] encrypted =
            encryptor.encrypt(
                plainText
            );

        String decrypted =
            encryptor.decrypt(
                encrypted
            );

        assertThat(decrypted)
            .isEqualTo(
                plainText
            );
    }

    @Test
    void 다른_키로_복호화하면_실패한다() {
        AesGcmPersonalDataEncryptor encryptor =
            new AesGcmPersonalDataEncryptor(
                VALID_KEY
            );

        AesGcmPersonalDataEncryptor otherEncryptor =
            new AesGcmPersonalDataEncryptor(
                OTHER_VALID_KEY
            );

        byte[] encrypted =
            encryptor.encrypt(
                "010-1234-5678"
            );

        assertThatThrownBy(
            () ->
                otherEncryptor.decrypt(
                    encrypted
                )
        )
            .isInstanceOf(
                PersonalDataEncryptionException.class
            );
    }

    @Test
    void 유효하지_않은_암호문을_복호화하면_실패한다() {
        AesGcmPersonalDataEncryptor encryptor =
            new AesGcmPersonalDataEncryptor(
                VALID_KEY
            );

        byte[] invalidEncryptedData =
            new byte[] {
                1
                , 2
                , 3
            };

        assertThatThrownBy(
            () ->
                encryptor.decrypt(
                    invalidEncryptedData
                )
        )
            .isInstanceOf(
                PersonalDataEncryptionException.class
            );
    }

    @Test
    void Base64가_아닌_키를_사용하면_객체_생성에_실패한다() {
        assertThatThrownBy(
            () ->
                new AesGcmPersonalDataEncryptor(
                    "not-base64-key!!"
                )
        )
            .isInstanceOf(
                IllegalStateException.class
            );
    }

    @Test
    void AES_256이_아닌_키를_사용하면_객체_생성에_실패한다() {
        String shortKey =
            Base64.getEncoder().encodeToString(
                "too-short"
                    .getBytes(StandardCharsets.UTF_8)
            );

        assertThatThrownBy(
            () ->
                new AesGcmPersonalDataEncryptor(
                    shortKey
                )
        )
            .isInstanceOf(
                IllegalStateException.class
            );
    }
}