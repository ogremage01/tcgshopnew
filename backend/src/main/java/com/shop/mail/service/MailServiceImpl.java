package com.shop.mail.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.shop.mail.exception.MailException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.password}")
    private String password;

    private final String verificationEmailTemplate = "template/verificationMailTemplate.html";
    private final String passwordResetEmailTemplate = "template/passwordResetMailTemplate.html";

    @Override
    public void sendVerificationEmail(String email) {
        try {
            // TODO: 인증 메일 발송
        } catch (Exception e) {
            throw new MailException(MailException.Code.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void sendPasswordResetEmail(String email) {
        try {
            // TODO: 비밀번호 재설정 메일 발송
        } catch (Exception e) {
            throw new MailException(MailException.Code.INTERNAL_SERVER_ERROR);
        }
    }
}
