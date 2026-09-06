package com.shop.log.point.support;

import com.shop.log.point.entity.PointLog;
import com.shop.log.point.entity.PointLogActorType;

/**
 * 포인트 로그 actorDetail → API 표시 문자열 변환.
 */
public final class PointLogPresenter {

    private PointLogPresenter() {
    }

    /** 관리자 화면용 실행자 표시 */
    public static String toAdminExecutorDisplay(PointLog pointLog) {
        PointLogActorType type = pointLog.resolveActorType();
        String detail = pointLog.getActorDetail();
        return switch (type) {
            case SYSTEM -> formatSystemDetail(detail);
            case ADMIN, USER -> formatAdminOrUserDetail(detail);
        };
    }

    private static String formatSystemDetail(String detail) {
        if (detail == null || detail.isBlank()) {
            return PointLogExecutorDisplay.SYSTEM;
        }
        return PointLogExecutorDisplay.SYSTEM + " (" + detail + ")";
    }

    private static String formatAdminOrUserDetail(String actorDetail) {
        if (actorDetail == null || actorDetail.isBlank()) {
            return PointLogExecutorDisplay.ADMIN;
        }
        return actorDetail;
    }
}
