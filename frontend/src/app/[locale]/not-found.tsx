"use client";

import ErrorFallback from "@/components/ErrorFallback";
import { routing } from "@/i18n/routing";
import { useParams } from "next/navigation";

export default function LocaleNotFound() {
  const params = useParams<{ locale: string }>();
  const locale = params?.locale ?? routing.defaultLocale;

  return (
    <ErrorFallback
      title="페이지를 찾을 수 없습니다."
      description="요청하신 페이지가 없거나 이동되었습니다."
      homeLabel="메인으로 돌아가기"
      homeHref={`/${locale}`}
    />
  );
}
