"use client";

import ErrorFallback from "@/components/ErrorFallback";

export default function ForbiddenPage() {
  return (
    <ErrorFallback
      title="관리자 페이지에 접근할 권한이 없습니다. / You do not have permission to access this admin page."
      description="관리자 계정으로 로그인한 뒤 다시 시도해 주세요. / Please sign in with an administrator account and try again."
      homeLabel="메인으로 이동 / Back to Home"
      homeHref="/"
    />
  );
}
