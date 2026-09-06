package com.shop.admin.alarm.controller;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.shop.admin.alarm.service.AdminAlarmService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/alarm")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSseController {

    private final AdminAlarmService adminAlarmService;

    @GetMapping(value = "/subscribe/{connectionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long connectionId) {
        return adminAlarmService.subscribe(connectionId);
    }
}
