package com.shop.mail.service;

public interface MailService {

    /**
     * 인증 메일 발송
     *
     * @param email 이메일
     */
    void sendVerificationEmail(String email);

    /**
     * 비밀번호 재설정 메일 발송
     *
     * @param email 이메일
     */
    void sendPasswordResetEmail(String email);
}
