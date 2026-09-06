package com.shop.scheduler.metadata.dto;

public record MetadataSyncResponse(MetadataSyncStatus status) {

    // 메타데이터 동기화 응답 Dto

    /**
     * 메타데이터 동기화 완료 응답
     * 
     * @return 메타데이터 동기화 완료 응답
     */
    public static MetadataSyncResponse completed() {
        return new MetadataSyncResponse(MetadataSyncStatus.COMPLETED);
    }

    /**
     * 메타데이터 동기화 실행 중 응답
     * 
     * @return 메타데이터 동기화 실행 중 응답
     */
    public static MetadataSyncResponse alreadyRunning() {
        return new MetadataSyncResponse(MetadataSyncStatus.ALREADY_RUNNING);
    }
}
