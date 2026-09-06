package com.shop.scheduler.price.source.openbinder;

/**
 * Open Binder(mtg-kr) 전체 동기화 결과.
 *
 * @param result  success | partial_success | failure
 * @param message 단계별 결과 (sync_log.message 저장용)
 */
public record OpenBinderSyncResult(String result, String message) {
}
