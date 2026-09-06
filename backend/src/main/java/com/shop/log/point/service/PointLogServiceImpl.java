package com.shop.log.point.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.shop.log.point.dto.PointLogDto;
import com.shop.log.point.dto.PointLogUserDto;
import com.shop.log.point.entity.PointLog;
import com.shop.log.point.entity.PointLogActorType;
import com.shop.log.point.repository.PointLogRepository;
import com.shop.log.point.support.PointLogExecutorDisplay;
import com.shop.log.point.support.PointLogPresenter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointLogServiceImpl implements PointLogService {

    private static final int RECENT_LIMIT = 10;

    private final PointLogRepository pointLogRepository;

    @Override
    public void savePointLog(PointLogDto pointLogDto) {
        pointLogRepository.save(PointLog.builder()
                .userId(pointLogDto.getUserId())
                .userName(pointLogDto.getUserName())
                .changedPoint(pointLogDto.getChangedPoint())
                .beforePoint(pointLogDto.getBeforePoint())
                .afterPoint(pointLogDto.getAfterPoint())
                .changeReason(pointLogDto.getChangeReason())
                .actorType(pointLogDto.getActorType())
                .actorDetail(pointLogDto.getActorDetail())
                .actionDate(pointLogDto.getActionDate())
                .isSuccess(pointLogDto.getIsSuccess())
                .build());
    }

    @Override
    public List<PointLogDto> getRecentPointLogsExcludeSystem() {
        return pointLogRepository.findTop50ByOrderByActionDateDesc().stream()
                .filter(p -> p.resolveActorType() != PointLogActorType.SYSTEM)
                .limit(RECENT_LIMIT)
                .map(this::toAdminDto)
                .toList();
    }

    @Override
    public List<PointLogDto> getRecentPointLogs() {
        return pointLogRepository.findTop10ByOrderByActionDateDesc().stream()
                .map(this::toAdminDto)
                .toList();
    }

    @Override
    public Page<PointLogDto> getPointLogsForAdmin(String keyword, boolean includeSystem, Pageable pageable) {
        return pointLogRepository.findAllByAdminFilter(keyword, includeSystem, pageable)
                .map(this::toAdminDto);
    }

    @Override
    public Page<PointLogUserDto> getPointLogsByUserIdForUser(Long userId, Pageable pageable) {
        return pointLogRepository.findByUserIdOrderByActionDateDesc(userId, pageable)
                .map(this::toUserDto);
    }

    private PointLogDto toAdminDto(PointLog pointLog) {
        return PointLogDto.builder()
                .id(pointLog.getId())
                .userId(pointLog.getUserId())
                .userName(pointLog.getUserName())
                .changedPoint(pointLog.getChangedPoint())
                .beforePoint(pointLog.getBeforePoint())
                .afterPoint(pointLog.getAfterPoint())
                .changeReason(pointLog.getChangeReason())
                .actorType(pointLog.resolveActorType())
                .actorDetail(pointLog.getActorDetail())
                .executor(PointLogPresenter.toAdminExecutorDisplay(pointLog))
                .actionDate(pointLog.getActionDate())
                .isSuccess(pointLog.getIsSuccess())
                .build();
    }

    private PointLogUserDto toUserDto(PointLog pointLog) {
        return PointLogUserDto.builder()
                .id(pointLog.getId())
                .changedPoint(pointLog.getChangedPoint())
                .beforePoint(pointLog.getBeforePoint())
                .afterPoint(pointLog.getAfterPoint())
                .changeReason(pointLog.getChangeReason())
                .executorDisplay(PointLogExecutorDisplay.forUser(pointLog))
                .actionDate(pointLog.getActionDate())
                .isSuccess(pointLog.getIsSuccess())
                .build();
    }
}
