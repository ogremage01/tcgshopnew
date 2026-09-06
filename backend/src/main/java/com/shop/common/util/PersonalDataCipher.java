package com.shop.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 개인정보(주소, 전화번호 등) AES-256-GCM 암호화/복호화.
 * 복호화 가능한 대칭 암호화만 수행하며, 키는 설정(app.personal-data-encryption-key)에서 주입.
 */
@Component
public class PersonalDataCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";// 암호화 알고리즘 지정
    private static final int GCM_TAG_LENGTH_BITS = 128;// 태그 길이 지정
    private static final int GCM_IV_LENGTH_BYTES = 12;// 초기화 벡터(IV) 길이 지정

    private final SecretKeySpec keySpec;// 키 지정

    // 생성자
    public PersonalDataCipher(
            @Value("${app.personal-data-encryption-key:}") String base64Key) {// 키 설정
        if (base64Key == null || base64Key.isBlank()) {// 키가 null 또는 빈 문자열인 경우 예외 발생
            throw new IllegalStateException(// 예외 발생 시 예외 메시지 반환
                    "app.personal-data-encryption-key must be set (e.g. PERSONAL_DATA_ENCRYPTION_KEY env). " +
                            "Use a Base64-encoded 32-byte value for AES-256.");// 예외 메시지 반환
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key.trim());// Base64로 디코딩하여 바이트 배열로 변환
        if (keyBytes.length != 32) {// 키 바이트 배열 길이가 32가 아닌 경우 예외 발생
            throw new IllegalStateException(// 예외 발생 시 예외 메시지 반환
                    "app.personal-data-encryption-key must decode to 32 bytes for AES-256, got " + keyBytes.length);// 예외
                                                                                                                    // 메시지
                                                                                                                    // 반환
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");// 키 지정
    }

    /**
     * 평문을 암호화하여 Base64 문자열로 반환.
     * 
     * @param plainText 평문
     * @return 암호화된 Base64 문자열
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            // 초기화 벡터(IV) 생성
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            java.security.SecureRandom.getInstanceStrong().nextBytes(iv); // 랜덤한 바이트 배열 생성

            Cipher cipher = Cipher.getInstance(ALGORITHM); // 암호화 알고리즘 인스턴스 생성
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv); // 초기화 벡터(IV)와 태그 길이 지정
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

            byte[] plain = plainText.getBytes(StandardCharsets.UTF_8); // 평문을 바이트 배열로 변환
            byte[] encrypted = cipher.doFinal(plain); // 암호화 수행

            byte[] combined = new byte[iv.length + encrypted.length]; // 초기화 벡터(IV)와 암호문을 결합
            System.arraycopy(iv, 0, combined, 0, iv.length); // 초기화 벡터(IV)를 결합 배열의 처음에 복사
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length); // 암호문을 결합 배열의 나머지 부분에 복사

            return Base64.getEncoder().encodeToString(combined); // 결합 배열을 Base64로 인코딩하여 문자열로 반환
        } catch (Exception e) { // 예외 처리
            throw new RuntimeException("Personal data encryption failed", e); // 예외 발생 시 예외 메시지와 원인 함께 예외 발생
        }
    }

    /**
     * Base64로 인코딩된 암호문을 복호화하여 평문 반환.
     */
    public String decrypt(String base64CipherText) {
        if (base64CipherText == null || base64CipherText.isEmpty()) { // 암호문이 null 또는 빈 문자열인 경우 원본 반환
            return base64CipherText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(base64CipherText); // Base64로 디코딩하여 바이트 배열로 변환
            if (combined.length <= GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Invalid cipher text length"); // 암호문 길이가 초기화 벡터(IV) 길이보다 짧은 경우 예외 발생
                                                                                  // 시 예외 메시지 반환
            }
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES]; // 초기화 벡터(IV) 배열 생성
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH_BYTES]; // 암호문 배열 생성
            System.arraycopy(combined, 0, iv, 0, iv.length); // 초기화 벡터(IV)를 배열의 처음에 복사
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length); // 암호문을 배열의 나머지 부분에 복사

            Cipher cipher = Cipher.getInstance(ALGORITHM); // 암호화 알고리즘 인스턴스 생성
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv); // 초기화 벡터(IV)와 태그 길이 지정
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec); // 복호화 모드로 초기화

            byte[] decrypted = cipher.doFinal(encrypted); // 복호화 수행
            return new String(decrypted, StandardCharsets.UTF_8); // 복호화된 바이트 배열을 문자열로 변환하여 반환
        } catch (Exception e) {
            throw new RuntimeException("Personal data decryption failed", e); // 예외 발생 시 예외 메시지와 원인 함께 예외 발생
        }
    }
}
