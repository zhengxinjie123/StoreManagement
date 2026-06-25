package com.joao.storemanagement.security;

import cn.hutool.core.util.StrUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 文本加密工具，用于保存第三方数据库连接密码等敏感配置。
 */
public final class TextEncryptor {

    private static final String PREFIX = "ENC:v1:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private TextEncryptor() {
    }

    public static String encrypt(String plainText, String secret) {
        if (StrUtil.isBlank(plainText)) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key(secret), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("文本加密失败", ex);
        }
    }

    public static String decrypt(String cipherText, String secret) {
        if (StrUtil.isBlank(cipherText)) {
            return cipherText;
        }
        if (!cipherText.startsWith(PREFIX)) {
            throw new IllegalStateException("密文格式不正确，无法解密");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(cipherText.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key(secret), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("文本解密失败", ex);
        }
    }

    private static SecretKeySpec key(String secret) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(StrUtil.nullToEmpty(secret).getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }
}
