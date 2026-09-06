package com.shop.user.controller;

// 탈퇴
// 정보 수정, 비밀번호 변경, 비밀번호 찾기, 비밀번호 재설정
// 비밀번호 재설정 이메일 발송, 비밀번호 재설정 이메일 인증
// 비밀번호 재설정 이메일 인증 코드 발송, 비밀번호 재설정 이메일 인증 코드 인증

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.user.dto.user.UserResponseDto;
import com.shop.user.exception.UserException;
import com.shop.user.service.user.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class UserController {
    // 회원 관련 기능을 담당한다. (회원 정보 조회, 탈퇴, 정보 수정, 비밀번호 변경, 비밀번호 찾기, 비밀번호 재설정, 비밀번호 재설정
    // 이메일 발송, 비밀번호 재설정 이메일 인증, 비밀번호 재설정 이메일 인증 코드 발송, 비밀번호 재설정 이메일 인증 코드 인증)

    private final UserService userService;

    /**
     * JWT subject(공개 ULID)로 내부 PK 조회. 관리자 경로는 별도 인증.
     */
    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return null;
        }
        return userService.findUserIdByPublicId(auth.getName()).orElse(null);
    }

    private ResponseEntity<Map<String, String>> unauthorizedResponse() {
        return ResponseEntity.status(401)
                .body(Map.of("messageCode", UserException.Code.UNAUTHORIZED.getMessageCode()));
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 현재 로그인 사용자 정보 (JWT로 식별, 응답에 id 미포함)
     * 
     * @return 현재 로그인 사용자 정보
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> me() {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        UserResponseDto user = userService.getUserById(userId);
        user.setId(null);
        return ResponseEntity.ok(user);
    }

    /**
     * 탈퇴 (본문 없음, JWT로 본인 식별)
     * 
     * @return 탈퇴 결과
     */
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, String>> withdraw() {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        userService.withdraw(userId);

        return ResponseEntity.ok(Map.of("message", "Withdrawn"));
    }

    /**
     * 정보 수정
     * 
     * @param body 정보 수정 요청 본문
     * @return 정보 수정 결과
     */
    @PostMapping("/change-name")
    public ResponseEntity<Map<String, String>> changeName(@RequestBody Map<String, String> body) {
        Long userId = currentUserId();
        if (userId == null) {
            return unauthorizedResponse();
        }

        String name = body.get("name");
        if (!hasText(name)) {
            return badRequest("Name is required");
        }

        userService.changeName(userId, name.trim());
        return ResponseEntity.ok(Map.of("message", "Name changed"));
    }

    /**
     * 비밀번호 변경
     * 
     * @param body 비밀번호 변경 요청 본문
     * @return 비밀번호 변경 결과
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> body) {
        Long userId = currentUserId();
        if (userId == null) {
            return unauthorizedResponse();
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = firstNonBlank(body.get("newPassword"), body.get("password"));
        String passwordConfirm = body.get("passwordConfirm");

        if (!hasText(currentPassword)) {
            return badRequest("Current password is required");
        }
        if (!hasText(newPassword)) {
            return badRequest("New password is required");
        }
        if (hasText(passwordConfirm) && !newPassword.equals(passwordConfirm)) {
            throw new UserException(UserException.Code.PASSWORD_NOT_MATCH);
        }

        userService.changePassword(userId, currentPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password changed"));
    }

    /**
     * 비밀번호 찾기
     * 
     * @param body 비밀번호 찾기 요청 본문
     * @return 비밀번호 찾기 결과
     */
    @PostMapping("/find-password")
    public ResponseEntity<Map<String, String>> findPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("message", "Password found"));
    }

    /**
     * 비밀번호 재설정
     * 
     * @param body 비밀번호 재설정 요청 본문
     * @return 비밀번호 재설정 결과
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("message", "Password reset"));
    }

    /**
     * 비밀번호 재설정 이메일 발송
     * 
     * @param body 비밀번호 재설정 이메일 발송 요청 본문
     * @return 비밀번호 재설정 이메일 발송 결과
     */
    @PostMapping("/send-email-reset-password-code")
    public ResponseEntity<Map<String, String>> sendEmailResetPasswordCode(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("message", "Email reset password code sent"));
    }

    /**
     * 비밀번호 재설정 이메일 인증
     * 
     * @param body 비밀번호 재설정 이메일 인증 요청 본문
     * @return 비밀번호 재설정 이메일 인증 결과
     */
    @PostMapping("/verify-email-reset-password-code")
    public ResponseEntity<Map<String, String>> verifyEmailResetPasswordCode(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("message", "Email reset password code verified"));
    }

    /**
     * 회원 정보 조회 (본인만 가능, JWT와 id 일치 시에만)
     * 
     * @param id 회원 ID
     * @return 회원 정보 조회 결과
     */
    @GetMapping("/{publicId}")
    public ResponseEntity<UserResponseDto> getUserInfo(@PathVariable String publicId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || !auth.getName().equals(publicId)) {
            return ResponseEntity.status(403).build();
        }
        UserResponseDto user = userService.getUserByPublicId(publicId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        user.setId(null);
        return ResponseEntity.ok(user);
    }
}
