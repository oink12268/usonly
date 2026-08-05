package com.evho.usonly.global.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

// 채팅 메시지 컬럼을 AES-256-GCM으로 암호화/복호화. JPA가 엔티티를 매핑할 때
// (native query 포함) 자동으로 적용되므로 다른 코드는 이 컨버터를 몰라도 됨.
//
// CHAT_ENCRYPTION_KEY 환경변수(base64, 32바이트)가 없으면 평문 그대로 통과시킨다
// (로컬 개발 편의). "enc:" 접두사로 암호문과 레거시 평문을 구분해서, 아직
// 마이그레이션 안 된 기존 평문 row를 만나도 깨진 문자로 복호화 실패하지 않고
// 그대로 반환한다.
@Slf4j
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String PREFIX = "enc:";
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private static final SecretKeySpec KEY = loadKey();

    private static SecretKeySpec loadKey() {
        String base64Key = System.getenv("CHAT_ENCRYPTION_KEY");
        if (base64Key == null || base64Key.isBlank()) {
            log.warn("CHAT_ENCRYPTION_KEY 미설정 - 채팅 메시지가 암호화되지 않습니다.");
            return null;
        }
        return new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
    }

    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null) return null;
        if (KEY == null) return plainText;

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, KEY, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("채팅 메시지 암호화 실패", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        if (KEY == null || !dbValue.startsWith(PREFIX)) return dbValue;

        try {
            byte[] combined = Base64.getDecoder().decode(dbValue.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, KEY, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("채팅 메시지 복호화 실패", e);
        }
    }
}
