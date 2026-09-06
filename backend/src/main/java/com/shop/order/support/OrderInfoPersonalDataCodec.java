package com.shop.order.support;

import org.springframework.stereotype.Component;

import com.shop.common.util.PersonalDataCipher;
import com.shop.order.entity.OrderInfo;

import lombok.RequiredArgsConstructor;

/**
 * {@link OrderInfo} 수령인 주소·전화번호·이메일 필드 암·복호화.
 * {@code recipientName} 은 평문으로 저장한다.
 */
@Component
@RequiredArgsConstructor
public class OrderInfoPersonalDataCodec {

    private final PersonalDataCipher personalDataCipher;

    public String encrypt(String plainText) {
        return personalDataCipher.encrypt(plainText);
    }

    /**
     * DB에 저장된 암호문을 평문으로 복원한다.
     * 기존 평문 데이터(마이그레이션 전)는 복호화 실패 시 원본을 그대로 반환한다.
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            return personalDataCipher.decrypt(cipherText);
        } catch (RuntimeException e) {
            return cipherText;
        }
    }
}
