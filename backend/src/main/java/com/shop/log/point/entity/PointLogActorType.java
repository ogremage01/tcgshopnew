package com.shop.log.point.entity;

/**
 * 포인트 변경 주체 유형. 고객 노출 문구는 {@link com.shop.log.point.support.PointLogExecutorDisplay}에서 결정한다.
 */
public enum PointLogActorType {

    /** 관리자 수동 조정 */
    ADMIN,

    /** 시스템 자동 처리 (주문 적립 등) */
    SYSTEM,

    /** 회원 본인 행위 (추후) */
    USER
}
