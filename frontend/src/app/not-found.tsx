"use client";

import ErrorFallback from "@/components/ErrorFallback";

export default function NotFound() {
  return (
    <ErrorFallback
      title="페이지를 찾을 수 없습니다."
      description="요청하신 페이지가 없거나 이동되었습니다."
      homeLabel="메인으로 돌아가기"
      homeHref="/"
    />
  );
}