package com.shop.mail.config;

import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;
import java.util.Properties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

// 메일 설정 application.yml 설정을 읽어옴
@Configuration
@RequiredArgsConstructor
public class MailConfig {

    // 메일 호스트
    @Value("${spring.mail.host}")
    private String host;

    // 메일 포트
    @Value("${spring.mail.port}")
    private int port;

    // 메일 사용자 이름
    @Value("${spring.mail.username}")
    private String username;

    // 메일 비밀번호
    @Value("${spring.mail.password}")
    private String password;

    // 메일 인증
    @Value("${spring.mail.properties.mail.smtp.auth}")
    private boolean auth;

    // 메일 TLS 활성화
    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private boolean starttlsEnable;

    // 메일 TLS 필수
    @Value("${spring.mail.properties.mail.smtp.starttls.required}")
    private boolean starttlsRequired;

    // 메일 연결 타임아웃
    @Value("${spring.mail.properties.mail.smtp.connectiontimeout}")
    private int connectiontimeout;

    // 메일 타임아웃
    @Value("${spring.mail.properties.mail.smtp.timeout}")
    private int timeout;

    // 메일 쓰기 타임아웃
    @Value("${spring.mail.properties.mail.smtp.writeTimeout}")
    private int writetimeout;

    // 메일 전송
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setJavaMailProperties(getMailProperties());
        return mailSender;
    }

    // 메일 속성 설정
    /**
     * 메일 속성 설정
     * 
     * @return 메일 속성
     */
    private Properties getMailProperties() {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.auth", String.valueOf(auth));
        properties.setProperty("mail.smtp.starttls.enable", String.valueOf(starttlsEnable));
        properties.setProperty("mail.smtp.starttls.required", String.valueOf(starttlsRequired));
        properties.setProperty("mail.smtp.connectiontimeout", String.valueOf(connectiontimeout));
        properties.setProperty("mail.smtp.timeout", String.valueOf(timeout));
        properties.setProperty("mail.smtp.writetimeout", String.valueOf(writetimeout));
        return properties;
    }

}
