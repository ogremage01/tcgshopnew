"use client";

import ErrorFallback from "@/components/ErrorFallback";
import { routing } from "@/i18n/routing";
import { useParams } from "next/navigation";

export default function LocaleError() {
  const params = useParams<{ locale: string }>();
  const locale = params?.locale ?? routing.defaultLocale;

  return (
    <ErrorFallback
      title="에러가 발생했습니다."
      description="관리자에게 문의해주세요."
      homeLabel="메인으로 돌아가기"
      homeHref={`/${locale}`}
    />
  );
}
