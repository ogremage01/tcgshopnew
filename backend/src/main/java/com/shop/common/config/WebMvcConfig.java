package com.shop.common.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.shop.security.AuthRateLimitInterceptor;

import lombok.RequiredArgsConstructor;

// 정적 리소스 매핑 설정
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthRateLimitInterceptor authRateLimitInterceptor;

    @Value("${file.path.upload:}")
    private String fileUploadRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드 루트가 비어 있으면 /uploads 정적 매핑을 만들지 않는다.
        if (fileUploadRoot == null || fileUploadRoot.isBlank()) {
            return;
        }
        // file.path.upload(예: d:/shop/uploads/)를 file:// URI로 변환해
        // Spring ResourceHandler가 OS 경로를 정적 리소스로 읽을 수 있게 한다.
        String location = Path.of(fileUploadRoot.trim().replace("\\", "/")).toUri().toString();
        // 디렉터리 리소스 기준으로 동작하도록 trailing slash를 보장한다.
        if (!location.endsWith("/")) {
            location += "/";
        }
        // /uploads/** 요청을 실제 업로드 디렉터리로 매핑한다.
        // 예: /uploads/banners/home/a.png -> d:/shop/uploads/banners/home/a.png
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authRateLimitInterceptor)
                .addPathPatterns("/api/auth/login", "/api/auth/register");
    }
}
