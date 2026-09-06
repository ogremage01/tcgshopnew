package com.shop.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.shop.auth.filter.GuestIdFilter;
import com.shop.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GuestIdFilter guestIdFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    @Value("${app.security.permit-metadata-without-auth:false}")
    private boolean permitMetadataWithoutAuth;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable()) // HTTP Basic Auth 비활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // 카드 썸네일 등 공개 정적 리소스 (spring.mvc.static-path-pattern: /card-images/**)
                    auth.requestMatchers("/card-images/**").permitAll();
                    auth.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll();
                    if (permitMetadataWithoutAuth) {
                        // local 개발 편의: 인증 없이 허용
                        auth.requestMatchers("/**").permitAll();
                    }
                    auth.requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/api/products/**").permitAll()
                            .requestMatchers("/api/guest/**").permitAll()
                            .requestMatchers("/api/checkout/toss/**").permitAll()
                            .requestMatchers("/api/**").authenticated()
                            .anyRequest().permitAll();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(guestIdFilter, JwtAuthenticationFilter.class);
        return http.build();
    }
}
