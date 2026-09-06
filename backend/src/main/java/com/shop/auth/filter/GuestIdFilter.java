package com.shop.auth.filter;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.shop.common.util.CookieUtils;
import com.shop.common.util.UlidGenerator;
import com.shop.security.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Component
@RequiredArgsConstructor
@Slf4j
public class GuestIdFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {

        log.debug("GuestIdFilter once request={}, thread={}", request.getRequestURI(), Thread.currentThread().getId());

        // 인증 정보 조회
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증된 사용자 여부 확인. 인증정보는 null도 아니고, 인증되었고, AnonymousAuthenticationToken이 아닌 경우.
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&!(authentication instanceof AnonymousAuthenticationToken);

        if(!isAuthenticated) {
            String guestId = CookieUtils.getCookieValue(request, "guestId");
            if(guestId == null) {
                guestId = jwtUtil.generateToken(UlidGenerator.nextUlid());

                CookieUtils.addCookie(response, "guestId", guestId, true, 30 * 24 * 60 * 60);
            }
            request.setAttribute("guestId", guestId);
        }

        filterChain.doFilter(request, response);
    }


}
