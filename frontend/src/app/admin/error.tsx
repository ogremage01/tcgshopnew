"use client";

import ErrorFallback from "@/components/ErrorFallback";

export default function AdminError() {
  return (
    <ErrorFallback
      title="관리자 페이지 오류가 발생했습니다."
      description="잠시 후 다시 시도하거나 관리자에게 문의해주세요."
      homeLabel="관리자 홈으로 이동"
      homeHref="/admin"
    />
  );
}
