package com.shop.order.adjustment.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.shop.log.point.dto.PointLogDto;
import com.shop.log.point.entity.PointLogActorType;
import com.shop.log.point.repository.PointLogRepository;
import com.shop.log.point.service.PointLogService;
import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.OrderAdjustmentType;
import com.shop.order.entity.OrderInfo;
import com.shop.order.point.OrderEarnedPointCalculator;
import com.shop.user.entity.User;
import com.shop.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderPointAdjustmentService {

    private final UserRepository userRepository;
    private final PointLogService pointLogService;
    private final PointLogRepository pointLogRepository;

    public void adjust(OrderAdjustmentContext ctx) {
        if (ctx.getType() == OrderAdjustmentType.FULL_CANCEL
                || isEmptyPartialCancel(ctx)) {
            adjustFullCancel(ctx);
        } else {
            adjustPartialModify(ctx);
        }
    }

    private boolean isEmptyPartialCancel(OrderAdjustmentContext ctx) {
        return ctx.getType() == OrderAdjustmentType.PARTIAL_MODIFY
                && (ctx.getRemainingLines() == null || ctx.getRemainingLines().isEmpty());
    }

    private void adjustFullCancel(OrderAdjustmentContext ctx) {
        OrderInfo orderInfo = ctx.getOrderInfo();
        if (orderInfo.getUserId() == null) {
            return;
        }
        User user = userRepository.findById(orderInfo.getUserId()).orElse(null);
        if (user == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        long currentPoint = user.getPoint() != null ? user.getPoint() : 0L;

        long usedPts = ctx.getOriginalUsedPointAmount() != null
                ? ctx.getOriginalUsedPointAmount().longValue() : 0L;
        long earnedPts = ctx.getOriginalEarnedPointAmount();

        if (usedPts > 0) {
            userRepository.addPoints(user.getId(), usedPts);
            saveLog(user, usedPts, currentPoint, currentPoint + usedPts,
                    "ORDER_CANCEL_REFUND:" + orderInfo.getId() , now);
            currentPoint += usedPts;
        }

        boolean pointsWereEarned = pointLogRepository.existsByChangeReason(
                OrderEarnedPointCalculator.earnChangeReason(orderInfo.getId()));
        if (pointsWereEarned && earnedPts > 0) {
            userRepository.addPoints(user.getId(), -earnedPts);
            saveLog(user, -earnedPts, currentPoint, currentPoint - earnedPts,
                    "ORDER_CANCEL_DEDUCT:" + orderInfo.getId(), now);
        }
    }

    private void adjustPartialModify(OrderAdjustmentContext ctx) {
        OrderInfo orderInfo = ctx.getOrderInfo();
        if (orderInfo.getUserId() == null) {
            return;
        }

        long releasedUsedPoints = ctx.getReleasedUsedPoints();
        if (releasedUsedPoints > 0) {
            User user = userRepository.findById(orderInfo.getUserId()).orElse(null);
            if (user != null) {
                long before = user.getPoint() != null ? user.getPoint() : 0L;
                userRepository.addPoints(user.getId(), releasedUsedPoints);
                saveLog(user, releasedUsedPoints, before, before + releasedUsedPoints,
                        "ORDER_MODIFY_REFUND:" + orderInfo.getId(), LocalDateTime.now());
            }
        }

        long earnedPointDelta = ctx.getEarnedPointDelta();
        if (earnedPointDelta <= 0) {
            return;
        }

        boolean pointsWereEarned = pointLogRepository.existsByChangeReason(
                OrderEarnedPointCalculator.earnChangeReason(orderInfo.getId()));
        if (!pointsWereEarned) {
            return;
        }

        User user = userRepository.findById(orderInfo.getUserId()).orElse(null);
        if (user == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        long currentPoint = user.getPoint() != null ? user.getPoint() : 0L;
        userRepository.addPoints(user.getId(), -earnedPointDelta);
        saveLog(user, -earnedPointDelta, currentPoint, currentPoint - earnedPointDelta,
                "ORDER_MODIFY_EARN_DEDUCT:" + orderInfo.getId(), now);
    }

    private void saveLog(User user, long changed, long before, long after, String reason, LocalDateTime now) {
        pointLogService.savePointLog(PointLogDto.builder()
                .userId(user.getId())
                .userName(user.getName())
                .changedPoint(changed)
                .beforePoint(before)
                .afterPoint(after)
                .changeReason(reason)
                .actorType(PointLogActorType.SYSTEM)
                .actorDetail(null)
                .actionDate(now)
                .isSuccess(true)
                .build());
    }
}
