"use client";

import ErrorFallback from "@/components/ErrorFallback";

export default function AdminNotFound() {
  return (
    <ErrorFallback
      title="관리자 페이지를 찾을 수 없습니다."
      description="주소를 확인하거나 관리자 홈으로 이동해주세요."
      homeLabel="관리자 홈으로 이동"
      homeHref="/admin"
    />
  );
}
