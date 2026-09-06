/**
 * 서버(SSR/API Route) 전용 API 베이스 URL.
 * - 브라우저는 NEXT_PUBLIC_API_URL 또는 빈 값(동일 오리진) 사용.
 * - Node에서는 상대 URL fetch가 불가하므로 절대 URL 필요.
 * 통합 Docker 시 컨테이너 내부에서 API_SERVER_URL=http://127.0.0.1:1567 로 설정.
 */
export function getServerApiBaseUrl(): string {
  return (
    process.env.API_SERVER_URL ??
    process.env.NEXT_PUBLIC_API_URL ??
    "http://127.0.0.1:18567"
  );
}
