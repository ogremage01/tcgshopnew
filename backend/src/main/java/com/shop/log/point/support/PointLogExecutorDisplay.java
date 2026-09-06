package com.shop.log.point.support;

import com.shop.log.point.entity.PointLog;
import com.shop.log.point.entity.PointLogActorType;

/**
 * 포인트 로그 실행자 표시 문구 (고객용 마스킹).
 */
public final class PointLogExecutorDisplay {

    public static final String ADMIN = "관리자";
    public static final String SYSTEM = "시스템";
    public static final String USER = "본인";

    private PointLogExecutorDisplay() {
    }

    public static String forUser(PointLogActorType actorType) {
        if (actorType == null) {
            return ADMIN;
        }
        return switch (actorType) {
            case SYSTEM -> SYSTEM;
            case USER -> USER;
            case ADMIN -> ADMIN;
        };
    }

    public static String forUser(PointLog pointLog) {
        return forUser(pointLog.resolveActorType());
    }
}
