package com.shop.log.point.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.log.point.dto.PointLogDto;
import com.shop.log.point.dto.PointLogUserDto;

public interface PointLogService {

    void savePointLog(PointLogDto pointLogDto);

    List<PointLogDto> getRecentPointLogsExcludeSystem();

    List<PointLogDto> getRecentPointLogs();

    Page<PointLogDto> getPointLogsForAdmin(String keyword, boolean includeSystem, Pageable pageable);

    Page<PointLogUserDto> getPointLogsByUserIdForUser(Long userId, Pageable pageable);
}
