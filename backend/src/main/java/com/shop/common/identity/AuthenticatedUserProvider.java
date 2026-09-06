package com.shop.common.identity;

import java.util.Optional;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.shop.user.entity.User;
import com.shop.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * SecurityContext의 JWT subject(공개 ULID)로 현재 로그인 사용자를 조회한다.
 * 관리자 API에서 로그·감사용 실행자 표시명을 만들 때 사용한다.
 */
@Component
@RequiredArgsConstructor
public class AuthenticatedUserProvider {

    private final UserRepository userRepository;

    /**
     * @return JWT subject(공개 ULID). 미인증이면 empty.
     */
    public Optional<String> getCurrentUserPublicId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        String publicId = auth.getName();
        if (publicId == null || publicId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(publicId);
    }

    /**
     * 포인트 로그 등에 기록할 실행자 표시명.
     *
     * @return 예: {@code 홍길동 (admin@example.com)}. 미인증·회원 없음이면 empty.
     */
    public Optional<String> resolveExecutorLabel() {
        return getCurrentUserPublicId()
                .flatMap(userRepository::findByPublicId)
                .map(this::formatExecutorLabel);
    }

    private String formatExecutorLabel(User user) {
        String name = user.getName();
        String email = user.getEmail();
        if (name != null && !name.isBlank() && email != null && !email.isBlank()) {
            return name + " (" + email + ")";
        }
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (email != null && !email.isBlank()) {
            return email;
        }
        return user.getPublicId();
    }

    /**
     * 현재 로그인 사용자 표시명(이름). 이름 없으면 이메일/publicId.
     */
    public Optional<String> resolveCurrentUserName() {
        return getCurrentUserPublicId()
                .flatMap(userRepository::findByPublicId)
                .map(user -> {
                    if (user.getName() != null && !user.getName().isBlank()) {
                        return user.getName();
                    }
                    if (user.getEmail() != null && !user.getEmail().isBlank()) {
                        return user.getEmail();
                    }
                    return user.getPublicId();
                });
    }
}
