package com.shop.log.user.service;

import com.shop.log.user.dto.UserLogDto;

public interface UserLogService {

    void saveUserLog(UserLogDto userLogDto);

    // Page<UserLogDto> getUserLogs(Pageable pageable);

    // Optional<UserLogDto> findTopByUserIdOrderByActionDateDesc(Long userId);

    // Page<UserLogDto> findAllByUserIdOrderByActionDateDesc(Long userId, Pageable pageable);
}
