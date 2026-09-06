package com.shop.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로그인·회원가입 엔드포인트에 IP 기반 요청 수 제한을 적용한다.
 * - /api/auth/login : 분당 10회
 * - /api/auth/register : 분당 5회
 */
@Component
public class AuthRateLimitInterceptor implements HandlerInterceptor {

    // 인증 요청 수 제한 인터셉터

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();

    private final int LOGIN_LIMIT = 10;
    private final int REGISTER_LIMIT = 5;
    private final Duration LOGIN_PERIOD = Duration.ofMinutes(1);
    private final Duration REGISTER_PERIOD = Duration.ofMinutes(1);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String uri = request.getRequestURI();
        String ip = resolveClientIp(request);

        Bucket bucket = null;
        if ("/api/auth/login".equals(uri)) {
            bucket = loginBuckets.computeIfAbsent(ip, k -> buildBucket(LOGIN_LIMIT, LOGIN_PERIOD));
        } else if ("/api/auth/register".equals(uri)) {
            bucket = registerBuckets.computeIfAbsent(ip, k -> buildBucket(REGISTER_LIMIT, REGISTER_PERIOD));
        }

        if (bucket != null && !bucket.tryConsume(1)) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Too many requests. Please try again later.\"}");
            return false;
        }
        return true;
    }

    /**
     * 버킷 생성
     * 
     * @param capacity 버킷 용량
     * @param period   버킷 주기
     * @return 버킷
     */
    private Bucket buildBucket(long capacity, Duration period) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(capacity, period)
                        .build())
                .build();
    }

    /**
     * 클라이언트 IP 추출
     * 
     * @param request 요청
     * @return 클라이언트 IP
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
