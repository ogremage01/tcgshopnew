package com.shop.log.user.service;

import org.springframework.stereotype.Service;

import com.shop.log.user.dto.UserLogDto;
import com.shop.log.user.entity.UserLog;
import com.shop.log.user.repository.UserLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserLogServiceImpl implements UserLogService {

    private final UserLogRepository userLogRepository;

    @Override
    public void saveUserLog(UserLogDto userLogDto) {
        userLogRepository.save(UserLog.builder()
                .category(userLogDto.getCategory())
                .userId(userLogDto.getUserId())
                .action(userLogDto.getAction())
                .actionDate(userLogDto.getActionDate())
                .userName(userLogDto.getUserName())
                .executor(userLogDto.getExecutor())
                .isSuccess(userLogDto.getIsSuccess())
                .build());
    }

}
