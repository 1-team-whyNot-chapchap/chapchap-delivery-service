package com.chapchap.delivery.global.security;

import com.chapchap.delivery.global.exception.technical.PersonalDataEncryptionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmPersonalDataEncryptor
    implements PersonalDataEncryptor {

    private static final String TRANSFORMATION =
        "AES/GCM/NoPadding";

    private static final String KEY_ALGORITHM =
        "AES";

    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;

    public AesGcmPersonalDataEncryptor(
        @Value("${security.personal-data.encryption-key}")
        String encodedKey
    ) {
        byte[] keyBytes = decodeKey(encodedKey);

        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                "Personal data encryption key must be 32 bytes."
            );
        }

        this.secretKey =
            new SecretKeySpec(keyBytes, KEY_ALGORITHM);

        this.secureRandom = new SecureRandom();
    }

    @Override
    public byte[] encrypt(String plainText) {
        if (plainText == null) {
            throw new IllegalArgumentException(
                "Plain text must not be null."
            );
        }

        try {
            byte[] iv = createIv();

            Cipher cipher =
                Cipher.getInstance(TRANSFORMATION);

            GCMParameterSpec parameterSpec =
                new GCMParameterSpec(
                    TAG_LENGTH_BITS
                    , iv
                );

            cipher.init(
                Cipher.ENCRYPT_MODE
                , secretKey
                , parameterSpec
            );

            byte[] encrypted =
                cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8)
                );

            return combine(iv, encrypted);
        } catch (GeneralSecurityException exception) {
            throw new PersonalDataEncryptionException(
                "Failed to encrypt personal data."
                , exception
            );
        }
    }

    private byte[] decodeKey(String encodedKey) {
        try {
            return Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                "Personal data encryption key must be Base64 encoded."
                , exception
            );
        }
    }

    private byte[] createIv() {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    private byte[] combine(
        byte[] iv
        , byte[] encrypted
    ) {
        return ByteBuffer
            .allocate(iv.length + encrypted.length)
            .put(iv)
            .put(encrypted)
            .array();
    }
}