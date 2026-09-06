package com.shop.user.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.log.point.dto.PointLogUserDto;
import com.shop.log.point.service.PointLogService;
import com.shop.user.service.user.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user/point-log")
@RequiredArgsConstructor
public class UserPointLogController {

    private final PointLogService pointLogService;
    private final UserService userService;

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return null;
        }
        return userService.findUserIdByPublicId(auth.getName()).orElse(null);
    }

    @GetMapping
    public ResponseEntity<Page<PointLogUserDto>> getPointLogs(Pageable pageable) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(pointLogService.getPointLogsByUserIdForUser(userId, pageable));
    }
}
