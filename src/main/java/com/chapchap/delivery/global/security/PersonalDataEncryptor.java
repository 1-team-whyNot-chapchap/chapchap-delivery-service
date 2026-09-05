package com.chapchap.delivery.global.security;

public interface PersonalDataEncryptor {

    byte[] encrypt(String plainText);

    String decrypt(byte[] encryptedData);
}