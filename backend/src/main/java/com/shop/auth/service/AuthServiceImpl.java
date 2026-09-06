package com.shop.auth.service;

import com.shop.auth.dto.AuthResponseDto;
import com.shop.auth.dto.AuthUserDto;
import com.shop.auth.dto.LoginRequestDto;
import com.shop.security.JwtUtil;
import com.shop.auth.refresh.entity.RefreshToken;
import com.shop.auth.refresh.repository.RefreshTokenRepository;
import com.shop.user.dto.user.UserResponseDto;
import com.shop.user.service.user.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.time.temporal.ChronoUnit;
import com.shop.auth.dto.RegisterRequestDto;
import com.shop.user.repository.UserRepository;
import com.shop.user.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.shop.auth.exception.AuthException;
import com.shop.auth.dto.UserCreateDto;
import com.shop.mail.service.MailService;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    // 비밀번호 제한 규약: 소문자, 대문자, 숫자, 특수문자 8자 이상
    // 아직은 미적용(해당 코드의 주석을 푸시오)
    private static final Pattern PASSWORD_POLICY = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    //private static final String PASSWORD_POLICY = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    @Value("${app.auth.refresh-expiration-ms:1209600000}")
    private long refreshTokenTtlMs;

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new AuthException(AuthException.Code.EMAIL_AND_PASSWORD_REQUIRED);
        }
        User user = userRepository.findByEmail(request.getEmail());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException(AuthException.Code.INVALID_EMAIL_OR_PASSWORD);
        }
        String accessToken = jwtUtil.generateToken(user.getPublicId());
        String refreshTokenRaw = generateRefreshTokenRaw();
        storeRefreshToken(user.getId(), refreshTokenRaw);
        return new AuthResponseDto(accessToken, AuthUserDto.fromEntity(user), refreshTokenRaw);
    }

    @Override
    public AuthResponseDto refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(AuthException.Code.INVALID_REFRESH_TOKEN);
        }
        String refreshTokenHash = hashRefreshToken(refreshToken);
        LocalDateTime now = LocalDateTime.now();

        RefreshToken stored = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(refreshTokenHash, now)
                .orElse(null);
        if (stored == null) {
            throw new AuthException(AuthException.Code.INVALID_REFRESH_TOKEN);
        }

        // rotate: 기존 refresh revoke + 새 refresh 발급
        stored.revoke(now);
        stored.rotate(now);
        refreshTokenRepository.save(stored);

        UserResponseDto user = userService.getUserById(stored.getUserId());
        if (user == null) {
            throw new AuthException(AuthException.Code.USER_NOT_FOUND);
        }

        String newAccessToken = jwtUtil.generateToken(user.getPublicId());
        String newRefreshTokenRaw = generateRefreshTokenRaw();
        storeRefreshToken(user.getId(), newRefreshTokenRaw);

        return new AuthResponseDto(newAccessToken, AuthUserDto.fromUserResponse(user), newRefreshTokenRaw);
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(AuthException.Code.INVALID_REFRESH_TOKEN);
        }
        String refreshTokenHash = hashRefreshToken(refreshToken);
        LocalDateTime now = LocalDateTime.now();

        refreshTokenRepository.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(refreshTokenHash, now)
                .ifPresent(rt -> {
                    rt.revoke(now);
                    refreshTokenRepository.save(rt);
                });
    }

    @Override
    public void register(RegisterRequestDto request) {
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new AuthException(AuthException.Code.DUPLICATE_EMAIL);
        }

        // 비밀번호 제한 규약: 소문자, 대문자, 숫자, 특수문자 8자 이상.
        // 개발중이므로 아직은 미적용(해당 코드의 주석을 푸시오)
        // if (!PASSWORD_POLICY.matcher(request.getPassword()).matches()) {
        //     throw new AuthException(AuthException.Code.PASSWORD_POLICY_VIOLATION);
        // }

        UserCreateDto userCreateDto = new UserCreateDto(request.getName(), request.getEmail(), request.getPassword(),
                true, LocalDateTime.now(), true, LocalDateTime.now(), "1.0", "1.0", "", "ACTIVE", "LOCAL", "ROLE_USER");
        
        userRepository.save(User.builder()
                .name(userCreateDto.getName())
                .email(userCreateDto.getEmail())
                .password(passwordEncoder.encode(userCreateDto.getPassword()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .point(0L)
                .userStatus("ACTIVE")
                .authProvider("LOCAL")
                .role("ROLE_USER")
                .termsAgreed(true)
                .termsAgreedAt(LocalDateTime.now())
                .privacyAgreed(true)
                .privacyAgreedAt(LocalDateTime.now())
                .privacyVersion("1.0")
                .termsVersion("1.0")
                .userMemo("")
                .build());
    }

    private void storeRefreshToken(Long userId, String refreshTokenRaw) {
        String tokenHash = hashRefreshToken(refreshTokenRaw);
        LocalDateTime now = LocalDateTime.now();
        RefreshToken entity = new RefreshToken(
                userId,
                tokenHash,
                now.plus(refreshTokenTtlMs, ChronoUnit.MILLIS),
                now);
        refreshTokenRepository.save(entity);
    }

    private String generateRefreshTokenRaw() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        // 쿠키/URL 안전을 위해 Base64 URL-safe
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashRefreshToken(String refreshTokenRaw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(refreshTokenRaw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            // SHA-256은 예외가 나지 않는 게 정상. 발생 시 안전하게 실패 처리
            throw new IllegalStateException("Failed to hash refresh token", e);
        }
    }

    @Override
    public void sendVerificationEmail(String email) {
        mailService.sendVerificationEmail(email);
    }
}
