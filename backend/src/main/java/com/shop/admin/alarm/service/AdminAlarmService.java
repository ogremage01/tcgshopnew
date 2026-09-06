package com.shop.admin.alarm.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.shop.admin.alarm.dto.AlarmDto;

public interface AdminAlarmService {
    SseEmitter subscribe(Long connectionId);

    void send(AlarmDto alarmDto);
}
