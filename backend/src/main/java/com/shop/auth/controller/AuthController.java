package com.shop.auth.controller;

import com.shop.auth.dto.AuthResponseDto;
import com.shop.auth.dto.LoginRequestDto;
import com.shop.auth.dto.RegisterRequestDto;
import com.shop.auth.service.AuthService;
import com.shop.cart.service.CartService;
import com.shop.common.util.CookieUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Map;
import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // 인증 관련 기능을 담당한다. (로그인, 토큰 갱신, 로그아웃, 회원가입)
    private static final String REFRESH_COOKIE_NAME = "refresh-token";
    private static final String ACCESS_COOKIE_NAME = "access-token";
    private static final String GUEST_ID_COOKIE_NAME = "guestId";

    private final AuthService authService;
    private final CartService cartService;

    @org.springframework.beans.factory.annotation.Value("${app.auth.refresh-expiration-ms:1209600000}")
    private long refreshTokenTtlMs;

    @org.springframework.beans.factory.annotation.Value("${app.auth.access-expiration-ms:3600000}")
    private long accessTokenTtlMs;

    // 운영은 app.cors.origins에 “하나만” 들어온다고 가정(사용자 확인).
    @org.springframework.beans.factory.annotation.Value("${app.cors.origins:http://localhost:3000}")
    private String corsOrigin;

    @org.springframework.beans.factory.annotation.Value("${app.security.strict-origin-check:true}")
    private boolean strictOriginCheck;

    /** {@code app.cors.origins}의 첫 origin URL (쉼표 구분 시 첫 항목, 파싱 실패 시 null). */
    private String getPrimaryCorsOrigin() {
        if (corsOrigin == null || corsOrigin.isBlank()) {
            return null;
        }
        return corsOrigin.split(",")[0].trim();
    }

    /** {@code app.cors.origins} URL에서 허용 host만 추출 (파싱 실패 시 null). */
    private String getAllowedHost() {
        try {
            String primary = getPrimaryCorsOrigin();
            if (primary == null || primary.isBlank()) {
                return null;
            }
            return URI.create(primary).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Origin/Referer 헤더가 허용 host와 다른지 판별합니다.
     * - 헤더가 없으면 false(의심 없음) 반환 (같은 origin 요청에서 헤더가 생략될 수 있음)
     * - 헤더가 있고 host가 다르면 true(의심) 반환
     */
    private boolean isOriginSuspect(HttpServletRequest request) {
        String allowedHost = getAllowedHost();
        if (allowedHost == null || allowedHost.isBlank()) {
            return false;
        }

        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            try {
                String originHost = URI.create(origin).getHost();
                if (originHost == null || !allowedHost.equalsIgnoreCase(originHost)) {
                    return true;
                }
            } catch (Exception e) {
                return true;
            }
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            try {
                String refererHost = URI.create(referer).getHost();
                if (refererHost != null && !allowedHost.equalsIgnoreCase(refererHost)) {
                    return true;
                }
            } catch (Exception e) {
                return true;
            }
        }

        return false;
    }

    /**
     * strictOriginCheck=true(운영): 의심 origin이면 FORBIDDEN 반환 (쿠키 클리어 포함)
     * strictOriginCheck=false(개발): 의심 origin이면 경고 로그만 남기고 통과
     *
     * @return 요청을 차단해야 하면 true
     */
    private ResponseCookie buildAccessCookie(String token, boolean secure) {
        return ResponseCookie.from(ACCESS_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(accessTokenTtlMs))
                .build();
    }

    private ResponseCookie clearAccessCookie(boolean secure) {
        return ResponseCookie.from(ACCESS_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie clearGuestIdCookie(boolean secure) {
        return ResponseCookie.from(GUEST_ID_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    /** 리프레시 토큰 쿠키 삭제. path는 발급 시 사용한 값과 동일해야 브라우저에서 제거된다. */
    private ResponseCookie clearRefreshCookie(boolean secure, String path) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(path)
                .maxAge(Duration.ZERO)
                .build();
    }

    /**
     * 로그인·리프레시 응답과 동일하게 path(/) 및 예전 로그인(path=/api/auth/refresh) 쿠키를 모두 무효화한다.
     */
    private void addClearRefreshTokenCookies(HttpHeaders headers, boolean secure) {
        headers.add(HttpHeaders.SET_COOKIE, clearRefreshCookie(secure, "/").toString());
        headers.add(HttpHeaders.SET_COOKIE, clearRefreshCookie(secure, "/api/auth/refresh").toString());
    }

    private boolean checkOriginAndBlock(HttpServletRequest request) {
        if (!isOriginSuspect(request)) {
            return false;
        }
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        if (strictOriginCheck) {
            log.warn("[SECURITY] Suspicious origin blocked. uri={} Origin={} Referer={}",
                    request.getRequestURI(), origin, referer);
            return true;
        } else {
            log.warn("[SECURITY][DEV] Suspicious origin detected (not blocked in dev). uri={} Origin={} Referer={}",
                    request.getRequestURI(), origin, referer);
            return false;
        }
    }

    /**
     * 로그인
     * 
     * @param request        로그인 요청
     * @param servletRequest 서블릿 요청
     * @return 로그인 결과
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest servletRequest) {
        AuthResponseDto response = authService.login(request);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("messageCode", "login.error"));
        }
        if (checkOriginAndBlock(servletRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String refreshTokenRaw = response.getRefreshToken();
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshTokenRaw)
                .httpOnly(true)
                .secure(servletRequest.isSecure())
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(refreshTokenTtlMs))
                .build();
        ResponseCookie accessCookie = buildAccessCookie(response.getToken(), servletRequest.isSecure());
        boolean secure = servletRequest.isSecure();

        String guestId = CookieUtils.getCookieValue(servletRequest, GUEST_ID_COOKIE_NAME);
        if (guestId != null && !guestId.isBlank()) {
            cartService.discardGuestCart(guestId);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearGuestIdCookie(secure).toString())
                .body(response);
    }

    /**
     * 토큰 갱신
     * 
     * @param refreshToken   리프레시 토큰
     * @param servletRequest 서블릿 요청
     * @return 토큰 갱신 결과
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest servletRequest) {
        ResponseCookie clearedRefresh = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(servletRequest.isSecure())
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        ResponseCookie clearedAccess = clearAccessCookie(servletRequest.isSecure());

        if (checkOriginAndBlock(servletRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .header(HttpHeaders.SET_COOKIE, clearedRefresh.toString())
                    .header(HttpHeaders.SET_COOKIE, clearedAccess.toString())
                    .build();
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, clearedRefresh.toString())
                    .header(HttpHeaders.SET_COOKIE, clearedAccess.toString())
                    .build();
        }
        AuthResponseDto response = authService.refresh(refreshToken);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, clearedRefresh.toString())
                    .header(HttpHeaders.SET_COOKIE, clearedAccess.toString())
                    .build();
        }

        String newRefreshToken = response.getRefreshToken();
        ResponseCookie newRefreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, newRefreshToken)
                .httpOnly(true)
                .secure(servletRequest.isSecure())
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(refreshTokenTtlMs))
                .build();
        ResponseCookie newAccessCookie = buildAccessCookie(response.getToken(), servletRequest.isSecure());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newRefreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, newAccessCookie.toString())
                .body(response);
    }

    /**
     * 로그아웃
     * 
     * @param refreshToken   리프레시 토큰
     * @param servletRequest 서블릿 요청
     * @return 로그아웃 결과
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest servletRequest) {
        boolean secure = servletRequest.isSecure();
        HttpHeaders cookieHeaders = new HttpHeaders();
        addClearRefreshTokenCookies(cookieHeaders, secure);
        cookieHeaders.add(HttpHeaders.SET_COOKIE, clearAccessCookie(secure).toString());

        if (checkOriginAndBlock(servletRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .headers(cookieHeaders)
                    .body(Map.of("message", "Forbidden"));
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        return ResponseEntity.ok()
                .headers(cookieHeaders)
                .body(Map.of("message", "Logged out"));
    }

    /**
     * 회원가입
     * 
     * @param request 회원가입 요청
     * @return 회원가입 결과
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequestDto request) {
        authService.register(request);
        return ResponseEntity.ok(Map.of("message", "Registered"));
    }

}
