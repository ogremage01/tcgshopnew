/**
 * SSE 구독 URL (same-origin).
 * `/api/admin/alarm/subscribe/...` → Route Handler가 백엔드 SSE를 스트리밍 프록시.
 *
 * 브라우저에서 API 서버(18567)로 직접 연결하면 CORS preflight가 막히므로
 * same-origin 프록시를 사용한다.
 */
export function buildAdminAlarmSubscribeUrl(connectionId: number): string {
  return `/api/admin/alarm/subscribe/${connectionId}`;
}
